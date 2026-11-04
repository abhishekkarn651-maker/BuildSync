package com.buildsync.messaging.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.messaging.application.ConversationService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/conversation")
@Tag(name = "Project Conversation", description = "Endpoints for accessing project team chat room")
@SecurityRequirement(name = "BearerAuth")
public class ProjectConversationController {

    private final ConversationService conversationService;

    public ProjectConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    @Operation(summary = "Get or create project conversation", description = "Retrieves or creates the group conversation for an active project. Owner and members only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Project conversation returned successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not an active member or owner", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Project not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationSummaryResponse> getProjectConversation(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ConversationSummaryResponse response = conversationService.getOrCreateProjectConversation(projectId, currentUserId);
        return ResponseEntity.ok(response);
    }
}

