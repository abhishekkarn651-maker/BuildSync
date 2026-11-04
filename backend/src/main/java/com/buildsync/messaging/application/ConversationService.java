package com.buildsync.messaging.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.messaging.api.*;
import com.buildsync.messaging.domain.Conversation;
import com.buildsync.messaging.domain.ConversationParticipant;
import com.buildsync.messaging.domain.ConversationType;
import com.buildsync.messaging.domain.Message;
import com.buildsync.messaging.infrastructure.ConversationParticipantRepository;
import com.buildsync.messaging.infrastructure.ConversationRepository;
import com.buildsync.messaging.infrastructure.MessageRepository;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.collaboration.domain.ProjectMember;
import com.buildsync.collaboration.infrastructure.ProjectMemberRepository;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectVisibility;
import com.buildsync.project.infrastructure.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ConversationService {

    private static final Logger log = LoggerFactory.getLogger(ConversationService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final DeveloperProfileRepository profileRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            DeveloperProfileRepository profileRepository,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Get or create a direct conversation between the authenticated developer and a target developer.
     */
    @Transactional
    public ConversationSummaryResponse getOrCreateDirectConversation(UUID currentUserId, String targetUsername) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

        User targetUser = userRepository.findByUsernameIgnoreCase(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found with username: " + targetUsername));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("Cannot start a conversation with yourself");
        }

        String key = Conversation.generateDirectKey(currentUser.getId(), targetUser.getId());

        Conversation conversation = conversationRepository.findByDirectConversationKey(key)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.createDirect(currentUser.getId(), targetUser.getId());
                    newConv = conversationRepository.save(newConv);

                    ConversationParticipant p1 = new ConversationParticipant(newConv, currentUser);
                    ConversationParticipant p2 = new ConversationParticipant(newConv, targetUser);
                    participantRepository.save(p1);
                    participantRepository.save(p2);

                    log.info("Created direct conversation between '{}' and '{}'", currentUser.getUsername(), targetUser.getUsername());
                    return newConv;
                });

        return mapToSummary(conversation, currentUserId);
    }

    /**
     * Get or create a project conversation. Only owner and active project members can access.
     */
    @Transactional
    public ConversationSummaryResponse getOrCreateProjectConversation(UUID projectId, UUID currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        boolean isOwner = project.isOwner(currentUserId);
        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId);

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have permission to access this project's conversation");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Conversation conversation = conversationRepository.findByProjectId(projectId)
                .orElseGet(() -> {
                    Conversation newConv = Conversation.createProject(project);
                    newConv = conversationRepository.save(newConv);

                    // Add project owner as participant
                    ConversationParticipant ownerParticipant = new ConversationParticipant(newConv, project.getOwner());
                    participantRepository.save(ownerParticipant);

                    // Add all current members as participants
                    List<ProjectMember> members = projectMemberRepository.findByProjectIdWithUser(projectId);
                    for (ProjectMember m : members) {
                        if (!m.getUser().getId().equals(project.getOwner().getId())) {
                            participantRepository.save(new ConversationParticipant(newConv, m.getUser()));
                        }
                    }

                    log.info("Created project conversation for project '{}'", project.getTitle());
                    return newConv;
                });

        // Ensure current user is in participants table if they just joined
        if (!participantRepository.existsByConversationIdAndUserId(conversation.getId(), currentUserId)) {
            participantRepository.save(new ConversationParticipant(conversation, currentUser));
        }

        return mapToSummary(conversation, currentUserId);
    }

    /**
     * Get paginated conversations for the current user.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ConversationSummaryResponse> getUserConversations(UUID currentUserId, Pageable pageable) {
        Page<Conversation> page = conversationRepository.findByParticipantUserId(currentUserId, pageable);

        List<ConversationSummaryResponse> summaries = page.getContent().stream()
                .filter(c -> hasAccess(c, currentUserId))
                .map(c -> mapToSummary(c, currentUserId))
                .toList();

        return new PagedResponse<>(
                summaries,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Get conversation details.
     */
    @Transactional(readOnly = true)
    public ConversationDetailResponse getConversationDetail(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findByIdWithDetails(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!hasAccess(conversation, currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this conversation");
        }

        ConversationProjectSummary projectSummary = null;
        if (conversation.getConversationType() == ConversationType.PROJECT && conversation.getProject() != null) {
            Project p = conversation.getProject();
            projectSummary = new ConversationProjectSummary(
                    p.getId(),
                    p.getTitle(),
                    p.getSlug(),
                    p.getVisibility().name(),
                    p.getCategory().name()
            );
        }

        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        List<ConversationParticipantSummary> participantSummaries = participants.stream()
                .map(p -> {
                    DeveloperProfile prof = profileRepository.findByUserId(p.getUser().getId()).orElse(null);
                    return new ConversationParticipantSummary(
                            p.getId(),
                            p.getUser().getId(),
                            p.getUser().getUsername(),
                            p.getUser().getFullName(),
                            prof != null ? prof.getAvatarUrl() : null,
                            p.getJoinedAt(),
                            p.getLastReadAt()
                    );
                })
                .toList();

        return new ConversationDetailResponse(
                conversation.getId(),
                conversation.getConversationType(),
                projectSummary,
                participantSummaries,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    /**
     * Mark a conversation as read by the authenticated user and emit read event.
     */
    @Transactional
    public void markAsRead(UUID conversationId, UUID currentUserId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!hasAccess(conversation, currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this conversation");
        }

        Instant now = Instant.now();
        participantRepository.updateLastReadAt(conversationId, currentUserId, now);

        User currentUser = userRepository.findById(currentUserId).orElse(null);
        String username = currentUser != null ? currentUser.getUsername() : "user";

        ReadReceiptEvent event = new ReadReceiptEvent(conversationId, currentUserId, username, now);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, event);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/read", event);
    }

    /**
     * Get total unread message count across all conversations for the user.
     */
    @Transactional(readOnly = true)
    public UnreadCountResponse getTotalUnreadCount(UUID currentUserId) {
        long count = messageRepository.countTotalUnreadMessagesForUser(currentUserId);
        return new UnreadCountResponse(count);
    }

    /**
     * Check if a user has access to a conversation by ID.
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID conversationId, UUID userId) {
        return conversationRepository.findById(conversationId)
                .map(c -> hasAccess(c, userId))
                .orElse(false);
    }

    /**
     * Dynamic authorization check:
     * - DIRECT: user is one of the participants.
     * - PROJECT: user is the project owner OR an active member in project_members.
     */
    public boolean hasAccess(Conversation conversation, UUID userId) {
        if (conversation == null || userId == null) return false;

        if (conversation.getConversationType() == ConversationType.DIRECT) {
            return participantRepository.existsByConversationIdAndUserId(conversation.getId(), userId);
        } else if (conversation.getConversationType() == ConversationType.PROJECT) {
            Project project = conversation.getProject();
            if (project == null) return false;
            boolean isOwner = project.isOwner(userId);
            boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(project.getId(), userId);
            return isOwner || isMember;
        }
        return false;
    }

    /**
     * Helper to map Conversation entity to ConversationSummaryResponse.
     */
    public ConversationSummaryResponse mapToSummary(Conversation conversation, UUID currentUserId) {
        ConversationProjectSummary projectSummary = null;
        MessageSenderSummary otherParticipant = null;

        if (conversation.getConversationType() == ConversationType.PROJECT && conversation.getProject() != null) {
            Project p = conversation.getProject();
            projectSummary = new ConversationProjectSummary(
                    p.getId(),
                    p.getTitle(),
                    p.getSlug(),
                    p.getVisibility().name(),
                    p.getCategory().name()
            );
        } else if (conversation.getConversationType() == ConversationType.DIRECT) {
            List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
            User otherUser = participants.stream()
                    .map(ConversationParticipant::getUser)
                    .filter(u -> !u.getId().equals(currentUserId))
                    .findFirst()
                    .orElse(null);

            if (otherUser != null) {
                DeveloperProfile prof = profileRepository.findByUserId(otherUser.getId()).orElse(null);
                otherParticipant = new MessageSenderSummary(
                        otherUser.getId(),
                        otherUser.getUsername(),
                        otherUser.getFullName(),
                        prof != null ? prof.getAvatarUrl() : null,
                        prof != null ? prof.getHeadline() : null
                );
            }
        }

        // Latest message preview
        MessagePreviewSummary lastMessage = messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(conversation.getId())
                .map(m -> new MessagePreviewSummary(
                        m.getId(),
                        m.getContent(),
                        m.getSender().getUsername(),
                        m.getCreatedAt()
                ))
                .orElse(null);

        // Unread count
        long unreadCount = 0;
        Optional<ConversationParticipant> myPart = participantRepository.findByConversationIdAndUserId(conversation.getId(), currentUserId);
        if (myPart.isPresent()) {
            unreadCount = messageRepository.countUnreadMessages(conversation.getId(), myPart.get().getLastReadAt(), currentUserId);
        }

        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getConversationType(),
                projectSummary,
                otherParticipant,
                lastMessage,
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}
