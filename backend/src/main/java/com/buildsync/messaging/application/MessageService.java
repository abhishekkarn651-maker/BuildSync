package com.buildsync.messaging.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.messaging.api.MessageResponse;
import com.buildsync.messaging.api.MessageSenderSummary;
import com.buildsync.messaging.api.TypingEvent;
import com.buildsync.messaging.domain.Conversation;
import com.buildsync.messaging.domain.Message;
import com.buildsync.messaging.domain.MessageType;
import com.buildsync.messaging.infrastructure.ConversationParticipantRepository;
import com.buildsync.messaging.infrastructure.ConversationRepository;
import com.buildsync.messaging.infrastructure.MessageRepository;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final DeveloperProfileRepository profileRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            UserRepository userRepository,
            DeveloperProfileRepository profileRepository,
            ConversationService conversationService,
            SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Persist a message and broadcast it in real time to all conversation participants.
     */
    @Transactional
    public MessageResponse sendMessage(UUID conversationId, UUID senderId, String content, MessageType messageType) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }
        if (content.length() > 4000) {
            throw new IllegalArgumentException("Message content cannot exceed 4000 characters");
        }

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!conversationService.hasAccess(conversation, senderId)) {
            throw new AccessDeniedException("You do not have permission to send messages in this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found"));

        MessageType type = messageType != null ? messageType : MessageType.TEXT;
        Message message = new Message(conversation, sender, content.trim(), type);
        Message saved = messageRepository.save(message);

        // Update conversation's updatedAt
        conversation.setUpdatedAt(saved.getCreatedAt());
        conversationRepository.save(conversation);

        // Update sender's lastReadAt
        participantRepository.updateLastReadAt(conversationId, senderId, saved.getCreatedAt());

        MessageResponse response = mapToResponse(saved);

        // Real-time broadcast to all subscribed participants
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
        log.debug("Broadcast message {} to /topic/conversations/{}", saved.getId(), conversationId);

        return response;
    }

    /**
     * Retrieve paginated message history in chronological display order.
     */
    @Transactional(readOnly = true)
    public PagedResponse<MessageResponse> getMessageHistory(UUID conversationId, UUID currentUserId, Pageable pageable) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        if (!conversationService.hasAccess(conversation, currentUserId)) {
            throw new AccessDeniedException("You do not have permission to view this conversation's messages");
        }

        Page<Message> page = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
        List<MessageResponse> responses = page.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PagedResponse<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Send ephemeral typing indicator event to participants.
     */
    public void sendTypingEvent(UUID conversationId, UUID userId, boolean typing) {
        if (!conversationService.hasAccess(conversationId, userId)) {
            throw new AccessDeniedException("You are not a participant in this conversation");
        }

        User user = userRepository.findById(userId).orElse(null);
        String username = user != null ? user.getUsername() : "user";

        TypingEvent event = new TypingEvent(conversationId, userId, username, typing, Instant.now());
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, event);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId + "/typing", event);
    }

    private MessageResponse mapToResponse(Message message) {
        User sender = message.getSender();
        DeveloperProfile profile = profileRepository.findByUserId(sender.getId()).orElse(null);
        String name = sender.getFullName();
        if (name == null || name.isBlank()) {
            name = sender.getUsername();
        }

        MessageSenderSummary senderSummary = new MessageSenderSummary(
                sender.getId(),
                sender.getUsername(),
                name,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getHeadline() : null
        );

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                senderSummary,
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }
}

