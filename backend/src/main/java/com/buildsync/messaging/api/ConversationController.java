package com.buildsync.messaging.api;

import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.messaging.application.ConversationService;
import com.buildsync.messaging.application.MessageService;
import com.buildsync.messaging.domain.MessageType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
@Tag(name = "Conversation Management", description = "Endpoints for direct/project conversations, message history, read receipts, and unread counts")
@SecurityRequirement(name = "BearerAuth")
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;

    public ConversationController(ConversationService conversationService, MessageService messageService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
    }

    @PostMapping("/direct")
    @Operation(summary = "Start or retrieve direct conversation", description = "Creates a direct conversation between two developers or returns the existing one.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direct conversation retrieved or created successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot start conversation with yourself", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Target developer not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationSummaryResponse> getOrCreateDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ConversationSummaryResponse response = conversationService.getOrCreateDirectConversation(currentUserId, request.username());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get user conversations", description = "Returns a paginated list of conversations the authenticated user participates in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversations retrieved successfully")
    })
    public ResponseEntity<PagedResponse<ConversationSummaryResponse>> getUserConversations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        PagedResponse<ConversationSummaryResponse> response = conversationService.getUserConversations(
                currentUserId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation details", description = "Retrieves metadata and participants for a specific conversation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conversation details retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a participant in this conversation", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ConversationDetailResponse> getConversationDetail(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ConversationDetailResponse response = conversationService.getConversationDetail(conversationId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "Get conversation message history", description = "Retrieves paginated messages for a conversation in chronological order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Messages retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a participant", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversation not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PagedResponse<MessageResponse>> getMessageHistory(
            @PathVariable UUID conversationId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        PagedResponse<MessageResponse> response = messageService.getMessageHistory(
                conversationId,
                currentUserId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Send message via REST", description = "Alternative HTTP endpoint to persist and broadcast a message.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid message content", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a participant", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessagePayload payload,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        MessageResponse response = messageService.sendMessage(conversationId, currentUserId, payload.content(), MessageType.TEXT);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{conversationId}/read")
    @Operation(summary = "Mark conversation as read", description = "Updates last-read timestamp cursor and broadcasts read receipt.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conversation marked as read"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Not a participant", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        conversationService.markAsRead(conversationId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get total unread message count", description = "Returns total count of unread messages across all conversations for the user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Unread count returned successfully")
    })
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        UnreadCountResponse response = conversationService.getTotalUnreadCount(currentUserId);
        return ResponseEntity.ok(response);
    }
}

