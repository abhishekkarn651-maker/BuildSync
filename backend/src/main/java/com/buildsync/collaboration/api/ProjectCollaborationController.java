package com.buildsync.collaboration.api;

import com.buildsync.collaboration.application.CollaborationService;
import com.buildsync.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for project-scoped collaboration actions, invites, and team member management.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}")
@Tag(name = "Project Collaboration & Teams", description = "Endpoints for project join requests, owner invitations, and team membership")
public class ProjectCollaborationController {

    private final CollaborationService collaborationService;

    public ProjectCollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @PostMapping("/join-requests")
    @Operation(
            summary = "Request to join project",
            description = "Submits a join request to a public project by the authenticated developer.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Join request submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or request already active", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborationRequestResponse> createJoinRequest(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody(required = false) CreateJoinRequest request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CollaborationRequestResponse response = collaborationService.createJoinRequest(projectId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/invitations")
    @Operation(
            summary = "Invite developer to project",
            description = "Invites a registered developer by username. Only the project owner can invite developers.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation sent successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or developer already in team", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only project owner can invite developers", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or developer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborationRequestResponse> createInvitation(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateInvitationRequest request) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CollaborationRequestResponse response = collaborationService.createInvitation(projectId, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/members")
    @Operation(
            summary = "Get project team members",
            description = "Retrieves all team members of a project including the owner. Public projects are accessible anonymously. Private projects are restricted to team members and owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team members retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Project not found or private", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ProjectMemberResponse>> getProjectMembers(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        List<ProjectMemberResponse> members = collaborationService.getProjectMembers(projectId, currentUserId);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/members/{userId}")
    @Operation(
            summary = "Remove team member",
            description = "Removes a member from the project. Only the project owner can remove members.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot remove project owner", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Only owner can remove members", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project or member not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> removeMember(
            @PathVariable("projectId") UUID projectId,
            @PathVariable("userId") UUID userId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        collaborationService.removeMember(projectId, userId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leave")
    @Operation(
            summary = "Leave project",
            description = "Allows an active team member to leave a project. The project owner cannot leave.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Successfully left the project"),
            @ApiResponse(responseCode = "400", description = "Owner cannot leave project or user is not a member", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> leaveProject(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        collaborationService.leaveProject(projectId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending-requests")
    @Operation(
            summary = "Get project pending requests",
            description = "Retrieves all pending join requests for a project. Project owner only.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Owner only", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CollaborationRequestResponse>> getProjectPendingRequests(
            @PathVariable("projectId") UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<CollaborationRequestResponse> requests = collaborationService.getProjectPendingRequests(projectId, currentUserId);
        return ResponseEntity.ok(requests);
    }
}
