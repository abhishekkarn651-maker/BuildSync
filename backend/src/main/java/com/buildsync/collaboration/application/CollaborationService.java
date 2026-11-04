package com.buildsync.collaboration.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.collaboration.api.CollaborationProjectSummary;
import com.buildsync.collaboration.api.CollaborationRequestResponse;
import com.buildsync.collaboration.api.CollaborationUserSummary;
import com.buildsync.collaboration.api.CreateInvitationRequest;
import com.buildsync.collaboration.api.CreateJoinRequest;
import com.buildsync.collaboration.api.ProjectMemberResponse;
import com.buildsync.collaboration.api.UserTeamResponse;
import com.buildsync.collaboration.domain.CollaborationRequest;
import com.buildsync.collaboration.domain.CollaborationRequestStatus;
import com.buildsync.collaboration.domain.CollaborationRequestType;
import com.buildsync.collaboration.domain.ProjectMember;
import com.buildsync.collaboration.domain.ProjectMemberRole;
import com.buildsync.collaboration.infrastructure.CollaborationRequestRepository;
import com.buildsync.collaboration.infrastructure.ProjectMemberRepository;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.project.api.ProjectOwnerResponse;
import com.buildsync.project.api.ProjectSummaryResponse;
import com.buildsync.project.api.ProjectTechnologyResponse;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectTechnology;
import com.buildsync.project.domain.ProjectVisibility;
import com.buildsync.project.infrastructure.ProjectRepository;
import com.buildsync.project.infrastructure.ProjectTechnologyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing project collaboration requests and team memberships.
 */
@Service
public class CollaborationService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationService.class);

    private final CollaborationRequestRepository collaborationRequestRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTechnologyRepository projectTechnologyRepository;
    private final UserRepository userRepository;
    private final DeveloperProfileRepository profileRepository;

    public CollaborationService(
            CollaborationRequestRepository collaborationRequestRepository,
            ProjectMemberRepository projectMemberRepository,
            ProjectRepository projectRepository,
            ProjectTechnologyRepository projectTechnologyRepository,
            UserRepository userRepository,
            DeveloperProfileRepository profileRepository) {
        this.collaborationRequestRepository = collaborationRequestRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.projectRepository = projectRepository;
        this.projectTechnologyRepository = projectTechnologyRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * Submit a join request to a public project.
     */
    @Transactional
    public CollaborationRequestResponse createJoinRequest(UUID projectId, UUID currentUserId, CreateJoinRequest request) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (project.getVisibility() != ProjectVisibility.PUBLIC) {
            throw new IllegalArgumentException("Join requests can only be submitted to public projects");
        }

        if (project.isOwner(currentUserId)) {
            throw new IllegalArgumentException("Project owners cannot request to join their own project");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId)) {
            throw new IllegalArgumentException("You are already an active team member of this project");
        }

        if (collaborationRequestRepository.existsByProjectIdAndRequesterIdAndStatus(projectId, currentUserId, CollaborationRequestStatus.PENDING)) {
            throw new IllegalStateException("A pending join request already exists for this project");
        }

        User requester = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        String sanitizedMessage = request != null && request.getMessage() != null ? request.getMessage().trim() : null;
        CollaborationRequest collabRequest = new CollaborationRequest(
                project,
                requester,
                project.getOwner(),
                CollaborationRequestType.JOIN_REQUEST,
                sanitizedMessage
        );

        CollaborationRequest saved = collaborationRequestRepository.save(collabRequest);
        log.info("Created join request id '{}' for project '{}' by user '{}'",
                saved.getId(), project.getTitle(), requester.getUsername());

        return mapToResponse(saved);
    }

    /**
     * Invite a developer to an owned project.
     */
    @Transactional
    public CollaborationRequestResponse createInvitation(UUID projectId, UUID currentUserId, CreateInvitationRequest request) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can invite developers");
        }

        String username = request.getUsername().trim();
        User invitee = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Developer not found with username: " + username));

        if (invitee.getId().equals(currentUserId)) {
            throw new IllegalArgumentException("You cannot invite yourself to collaborate on your own project");
        }

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())) {
            throw new IllegalArgumentException("Developer is already an active team member of this project");
        }

        if (collaborationRequestRepository.existsByProjectIdAndRecipientIdAndStatus(projectId, invitee.getId(), CollaborationRequestStatus.PENDING)) {
            throw new IllegalStateException("A pending invitation already exists for this developer");
        }

        String sanitizedMessage = request.getMessage() != null ? request.getMessage().trim() : null;
        CollaborationRequest collabRequest = new CollaborationRequest(
                project,
                project.getOwner(),
                invitee,
                CollaborationRequestType.INVITATION,
                sanitizedMessage
        );

        CollaborationRequest saved = collaborationRequestRepository.save(collabRequest);
        log.info("Created invitation id '{}' for project '{}' to user '{}'",
                saved.getId(), project.getTitle(), invitee.getUsername());

        return mapToResponse(saved);
    }

    /**
     * Get paginated incoming requests for the authenticated user.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CollaborationRequestResponse> getIncomingRequests(UUID currentUserId, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = (size <= 0 || size > 50) ? 10 : size;

        Page<CollaborationRequest> requestPage = collaborationRequestRepository
                .findByRecipientIdWithDetails(currentUserId, PageRequest.of(pageNumber, pageSize));

        List<CollaborationRequestResponse> content = mapRequestsWithProfiles(requestPage.getContent());

        return new PagedResponse<>(
                content,
                requestPage.getNumber(),
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages(),
                requestPage.isLast()
        );
    }

    /**
     * Get paginated outgoing requests sent by the authenticated user.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CollaborationRequestResponse> getOutgoingRequests(UUID currentUserId, int page, int size) {
        int pageNumber = Math.max(0, page);
        int pageSize = (size <= 0 || size > 50) ? 10 : size;

        Page<CollaborationRequest> requestPage = collaborationRequestRepository
                .findByRequesterIdWithDetails(currentUserId, PageRequest.of(pageNumber, pageSize));

        List<CollaborationRequestResponse> content = mapRequestsWithProfiles(requestPage.getContent());

        return new PagedResponse<>(
                content,
                requestPage.getNumber(),
                requestPage.getSize(),
                requestPage.getTotalElements(),
                requestPage.getTotalPages(),
                requestPage.isLast()
        );
    }

    /**
     * Accept a collaboration request and create project membership atomically.
     */
    @Transactional
    public CollaborationRequestResponse acceptRequest(UUID requestId, UUID currentUserId) {
        CollaborationRequest request = collaborationRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found with id: " + requestId));

        if (request.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new IllegalStateException("Request cannot be accepted because it is in " + request.getStatus() + " status");
        }

        User userToAddAsMember;
        if (request.getRequestType() == CollaborationRequestType.JOIN_REQUEST) {
            // Only project owner can accept join requests
            if (!request.getProject().isOwner(currentUserId)) {
                throw new AccessDeniedException("Only the project owner can accept join requests");
            }
            userToAddAsMember = request.getRequester();
        } else {
            // Only invited developer can accept invitations
            if (!request.getRecipient().getId().equals(currentUserId)) {
                throw new AccessDeniedException("Only the invited developer can accept this invitation");
            }
            userToAddAsMember = request.getRecipient();
        }

        // Add team membership if not already a member (concurrency protection)
        if (!projectMemberRepository.existsByProjectIdAndUserId(request.getProject().getId(), userToAddAsMember.getId())) {
            ProjectMember member = new ProjectMember(request.getProject(), userToAddAsMember, ProjectMemberRole.MEMBER);
            projectMemberRepository.save(member);
            log.info("Added user '{}' as team member to project '{}'", userToAddAsMember.getUsername(), request.getProject().getTitle());
        }

        request.setStatus(CollaborationRequestStatus.ACCEPTED);
        request.setRespondedAt(Instant.now());
        CollaborationRequest updated = collaborationRequestRepository.save(request);

        return mapToResponse(updated);
    }

    /**
     * Reject a collaboration request.
     */
    @Transactional
    public CollaborationRequestResponse rejectRequest(UUID requestId, UUID currentUserId) {
        CollaborationRequest request = collaborationRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found with id: " + requestId));

        if (request.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new IllegalStateException("Request cannot be rejected because it is in " + request.getStatus() + " status");
        }

        if (request.getRequestType() == CollaborationRequestType.JOIN_REQUEST) {
            // Only project owner can reject join requests
            if (!request.getProject().isOwner(currentUserId)) {
                throw new AccessDeniedException("Only the project owner can reject join requests");
            }
        } else {
            // Only invited developer can reject invitations
            if (!request.getRecipient().getId().equals(currentUserId)) {
                throw new AccessDeniedException("Only the invited developer can reject this invitation");
            }
        }

        request.setStatus(CollaborationRequestStatus.REJECTED);
        request.setRespondedAt(Instant.now());
        CollaborationRequest updated = collaborationRequestRepository.save(request);
        log.info("Rejected collaboration request id '{}' by user id '{}'", requestId, currentUserId);

        return mapToResponse(updated);
    }

    /**
     * Cancel an active pending collaboration request.
     */
    @Transactional
    public CollaborationRequestResponse cancelRequest(UUID requestId, UUID currentUserId) {
        CollaborationRequest request = collaborationRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Collaboration request not found with id: " + requestId));

        if (request.getStatus() != CollaborationRequestStatus.PENDING) {
            throw new IllegalStateException("Request cannot be cancelled because it is in " + request.getStatus() + " status");
        }

        boolean isRequester = request.getRequester().getId().equals(currentUserId);
        boolean isOwner = request.getProject().isOwner(currentUserId);

        if (!isRequester && !isOwner) {
            throw new AccessDeniedException("You do not have permission to cancel this collaboration request");
        }

        request.setStatus(CollaborationRequestStatus.CANCELLED);
        request.setRespondedAt(Instant.now());
        CollaborationRequest updated = collaborationRequestRepository.save(request);
        log.info("Cancelled collaboration request id '{}' by user id '{}'", requestId, currentUserId);

        return mapToResponse(updated);
    }

    /**
     * Retrieve all team members for a project (Owner + Members).
     */
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId, UUID currentUserId) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Privacy check for private projects
        if (project.getVisibility() == ProjectVisibility.PRIVATE) {
            boolean isAuthorized = currentUserId != null &&
                    (project.isOwner(currentUserId) || projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId));
            if (!isAuthorized) {
                throw new ResourceNotFoundException("Project not found with id: " + projectId);
            }
        }

        List<ProjectMember> nonOwnerMembers = projectMemberRepository.findByProjectIdWithUser(projectId);

        // Batch gather profiles
        Set<UUID> allUserIds = new HashSet<>();
        allUserIds.add(project.getOwner().getId());
        nonOwnerMembers.forEach(m -> allUserIds.add(m.getUser().getId()));

        Map<UUID, DeveloperProfile> profileMap = !allUserIds.isEmpty()
                ? profileRepository.findByUserIdInWithUser(allUserIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p))
                : Collections.emptyMap();

        List<ProjectMemberResponse> result = new ArrayList<>();

        // 1. Add Owner
        DeveloperProfile ownerProfile = profileMap.get(project.getOwner().getId());
        result.add(new ProjectMemberResponse(
                null,
                project.getOwner().getId(),
                project.getOwner().getUsername(),
                project.getOwner().getFullName(),
                ownerProfile != null ? ownerProfile.getAvatarUrl() : null,
                ownerProfile != null ? ownerProfile.getHeadline() : null,
                "OWNER",
                true,
                project.getCreatedAt()
        ));

        // 2. Add Members
        for (ProjectMember member : nonOwnerMembers) {
            DeveloperProfile memberProfile = profileMap.get(member.getUser().getId());
            result.add(new ProjectMemberResponse(
                    member.getId(),
                    member.getUser().getId(),
                    member.getUser().getUsername(),
                    member.getUser().getFullName(),
                    memberProfile != null ? memberProfile.getAvatarUrl() : null,
                    memberProfile != null ? memberProfile.getHeadline() : null,
                    member.getRole().name(),
                    false,
                    member.getJoinedAt()
            ));
        }

        return result;
    }

    /**
     * Remove a member from the project. Owner only.
     */
    @Transactional
    public void removeMember(UUID projectId, UUID targetUserId, UUID currentUserId) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can remove team members");
        }

        if (project.isOwner(targetUserId)) {
            throw new IllegalArgumentException("Cannot remove the project owner from their project");
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUserId)) {
            throw new ResourceNotFoundException("Team member not found in this project");
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, targetUserId);
        log.info("Owner user id '{}' removed member user id '{}' from project '{}'",
                currentUserId, targetUserId, project.getTitle());
    }

    /**
     * Leave a project. Non-owner members only.
     */
    @Transactional
    public void leaveProject(UUID projectId, UUID currentUserId) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (project.isOwner(currentUserId)) {
            throw new IllegalArgumentException("Project owners cannot leave their own project");
        }

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId)) {
            throw new IllegalArgumentException("You are not an active member of this project");
        }

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, currentUserId);
        log.info("User id '{}' voluntarily left project '{}'", currentUserId, project.getTitle());
    }

    /**
     * Get all project teams where the current user is an owner or active member.
     */
    @Transactional(readOnly = true)
    public List<UserTeamResponse> getUserTeams(UUID currentUserId) {
        // Projects owned by user
        List<Project> ownedProjects = projectRepository.findByOwnerIdWithOwnerOrderByUpdatedAtDesc(currentUserId);

        // Memberships for user
        List<ProjectMember> memberships = projectMemberRepository.findByUserIdWithProject(currentUserId);

        // Gather all project IDs to batch fetch technologies and member counts
        Set<UUID> allProjectIds = new HashSet<>();
        ownedProjects.forEach(p -> allProjectIds.add(p.getId()));
        memberships.forEach(m -> allProjectIds.add(m.getProject().getId()));

        if (allProjectIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Batch fetch technologies
        List<ProjectTechnology> allTechs = projectTechnologyRepository.findByProjectIdInWithSkill(allProjectIds);
        Map<UUID, List<ProjectTechnology>> techMap = allTechs.stream()
                .collect(Collectors.groupingBy(pt -> pt.getProject().getId()));

        // Batch fetch owner profiles
        Set<UUID> ownerIds = new HashSet<>();
        ownedProjects.forEach(p -> ownerIds.add(p.getOwner().getId()));
        memberships.forEach(m -> ownerIds.add(m.getProject().getOwner().getId()));

        Map<UUID, DeveloperProfile> ownerProfileMap = !ownerIds.isEmpty()
                ? profileRepository.findByUserIdInWithUser(ownerIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p))
                : Collections.emptyMap();

        List<UserTeamResponse> responses = new ArrayList<>();
        Set<UUID> processedProjectIds = new HashSet<>();

        // 1. Process owned projects
        for (Project project : ownedProjects) {
            processedProjectIds.add(project.getId());
            long memberCount = projectMemberRepository.countByProjectId(project.getId()) + 1; // +1 for owner
            DeveloperProfile ownerProfile = ownerProfileMap.get(project.getOwner().getId());
            ProjectSummaryResponse summary = mapToProjectSummary(project, techMap.get(project.getId()), ownerProfile);
            responses.add(new UserTeamResponse(summary, "OWNER", true, memberCount, project.getCreatedAt()));
        }

        // 2. Process memberships
        for (ProjectMember membership : memberships) {
            Project project = membership.getProject();
            if (processedProjectIds.add(project.getId())) {
                long memberCount = projectMemberRepository.countByProjectId(project.getId()) + 1;
                DeveloperProfile ownerProfile = ownerProfileMap.get(project.getOwner().getId());
                ProjectSummaryResponse summary = mapToProjectSummary(project, techMap.get(project.getId()), ownerProfile);
                responses.add(new UserTeamResponse(summary, membership.getRole().name(), false, memberCount, membership.getJoinedAt()));
            }
        }

        return responses;
    }

    /**
     * Get pending join requests for a project (Owner only).
     */
    @Transactional(readOnly = true)
    public List<CollaborationRequestResponse> getProjectPendingRequests(UUID projectId, UUID currentUserId) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can view pending collaboration requests");
        }

        List<CollaborationRequest> pending = collaborationRequestRepository.findPendingByProjectId(projectId);
        return mapRequestsWithProfiles(pending);
    }

    // Helper mapping methods

    private List<CollaborationRequestResponse> mapRequestsWithProfiles(List<CollaborationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> userIds = new HashSet<>();
        requests.forEach(r -> {
            userIds.add(r.getRequester().getId());
            userIds.add(r.getRecipient().getId());
        });

        Map<UUID, DeveloperProfile> profileMap = !userIds.isEmpty()
                ? profileRepository.findByUserIdInWithUser(userIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p))
                : Collections.emptyMap();

        return requests.stream()
                .map(r -> mapToResponseWithProfiles(r, profileMap))
                .toList();
    }

    private CollaborationRequestResponse mapToResponse(CollaborationRequest request) {
        Set<UUID> userIds = Set.of(request.getRequester().getId(), request.getRecipient().getId());
        Map<UUID, DeveloperProfile> profileMap = profileRepository.findByUserIdInWithUser(userIds).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
        return mapToResponseWithProfiles(request, profileMap);
    }

    private CollaborationRequestResponse mapToResponseWithProfiles(
            CollaborationRequest request, Map<UUID, DeveloperProfile> profileMap) {

        DeveloperProfile reqProfile = profileMap.get(request.getRequester().getId());
        DeveloperProfile recProfile = profileMap.get(request.getRecipient().getId());

        CollaborationProjectSummary projectSummary = new CollaborationProjectSummary(
                request.getProject().getId(),
                request.getProject().getSlug(),
                request.getProject().getTitle(),
                request.getProject().getShortDescription(),
                request.getProject().getCategory(),
                request.getProject().getStatus(),
                request.getProject().getVisibility()
        );

        CollaborationUserSummary requesterSummary = new CollaborationUserSummary(
                request.getRequester().getId(),
                request.getRequester().getUsername(),
                request.getRequester().getFullName(),
                reqProfile != null ? reqProfile.getAvatarUrl() : null,
                reqProfile != null ? reqProfile.getHeadline() : null
        );

        CollaborationUserSummary recipientSummary = new CollaborationUserSummary(
                request.getRecipient().getId(),
                request.getRecipient().getUsername(),
                request.getRecipient().getFullName(),
                recProfile != null ? recProfile.getAvatarUrl() : null,
                recProfile != null ? recProfile.getHeadline() : null
        );

        return new CollaborationRequestResponse(
                request.getId(),
                projectSummary,
                requesterSummary,
                recipientSummary,
                request.getRequestType(),
                request.getStatus(),
                request.getMessage(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getRespondedAt()
        );
    }

    private ProjectSummaryResponse mapToProjectSummary(
            Project project, List<ProjectTechnology> techs, DeveloperProfile ownerProfile) {

        List<ProjectTechnologyResponse> techResponses = techs != null
                ? techs.stream().map(pt -> ProjectTechnologyResponse.fromSkill(pt.getSkill())).toList()
                : Collections.emptyList();

        ProjectOwnerResponse ownerResponse = new ProjectOwnerResponse(
                project.getOwner().getId(),
                project.getOwner().getUsername(),
                project.getOwner().getFullName(),
                ownerProfile != null ? ownerProfile.getAvatarUrl() : null,
                ownerProfile != null ? ownerProfile.getHeadline() : null
        );

        return new ProjectSummaryResponse(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getShortDescription(),
                project.getCategory(),
                project.getStatus(),
                project.getVisibility(),
                project.getGithubUrl(),
                project.getDemoUrl(),
                project.getStartDate(),
                project.getCompletionDate(),
                ownerResponse,
                techResponses,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
