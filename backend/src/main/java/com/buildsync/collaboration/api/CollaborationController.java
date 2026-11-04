package com.buildsync.collaboration.api;

import com.buildsync.collaboration.application.CollaborationService;
import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for developer collaboration requests and personal team overview.
 */
@RestController
@RequestMapping("/api/v1/collaborations")
@Tag(name = "Collaboration Management", description = "Endpoints for managing incoming/outgoing collaboration requests, actions, and user project teams")
@SecurityRequirement(name = "BearerAuth")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @GetMapping("/incoming")
    @Operation(
            summary = "Get incoming collaboration requests",
            description = "Retrieves paginated collaboration requests where the authenticated user is the recipient (join requests for owned projects or invitations to collaborate)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incoming requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagedResponse<CollaborationRequestResponse>> getIncomingRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        PagedResponse<CollaborationRequestResponse> response = collaborationService.getIncomingRequests(currentUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/outgoing")
    @Operation(
            summary = "Get outgoing collaboration requests",
            description = "Retrieves paginated collaboration requests sent by the authenticated developer (sent join requests or sent owner invitations)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Outgoing requests retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagedResponse<CollaborationRequestResponse>> getOutgoingRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        PagedResponse<CollaborationRequestResponse> response = collaborationService.getOutgoingRequests(currentUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-teams")
    @Operation(
            summary = "Get user project teams",
            description = "Retrieves all projects where the authenticated developer is an owner or active team member."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User teams retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserTeamResponse>> getMyTeams(@AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        List<UserTeamResponse> response = collaborationService.getUserTeams(currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/accept")
    @Operation(
            summary = "Accept collaboration request",
            description = "Accepts a pending collaboration request and creates an active project membership transactionally."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request accepted and membership created"),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING state", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to accept this request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborationRequestResponse> acceptRequest(
            @PathVariable("requestId") UUID requestId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CollaborationRequestResponse response = collaborationService.acceptRequest(requestId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/reject")
    @Operation(
            summary = "Reject collaboration request",
            description = "Rejects a pending collaboration request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING state", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to reject this request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborationRequestResponse> rejectRequest(
            @PathVariable("requestId") UUID requestId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CollaborationRequestResponse response = collaborationService.rejectRequest(requestId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{requestId}/cancel")
    @Operation(
            summary = "Cancel collaboration request",
            description = "Cancels a pending request. Can only be performed by the requester or project owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING state", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not authorized to cancel this request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborationRequestResponse> cancelRequest(
            @PathVariable("requestId") UUID requestId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        CollaborationRequestResponse response = collaborationService.cancelRequest(requestId, currentUserId);
        return ResponseEntity.ok(response);
    }
}
