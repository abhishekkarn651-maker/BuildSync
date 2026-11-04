package com.buildsync.messaging.api;

import com.buildsync.messaging.application.MessageService;
import com.buildsync.messaging.domain.MessageType;
import com.buildsync.messaging.websocket.UserPrincipal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * STOMP controller handling real-time incoming messages and typing indicator events.
 */
@Controller
public class MessageRealtimeController {

    private static final Logger log = LoggerFactory.getLogger(MessageRealtimeController.class);

    private final MessageService messageService;

    public MessageRealtimeController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Handle incoming real-time chat message.
     * Destination: /app/conversations/{conversationId}/send
     */
    @MessageMapping("/conversations/{conversationId}/send")
    public void handleSendMessage(
            @DestinationVariable UUID conversationId,
            @Valid @Payload SendMessagePayload payload,
            Principal principal) {
        UserPrincipal userPrincipal = extractUserPrincipal(principal);
        if (userPrincipal == null) {
            log.warn("Unauthenticated STOMP send attempt to conversation: {}", conversationId);
            throw new AccessDeniedException("Authentication required to send messages");
        }

        log.debug("Received STOMP message from '{}' for conversation '{}'", userPrincipal.getName(), conversationId);
        messageService.sendMessage(conversationId, userPrincipal.getUserId(), payload.content(), MessageType.TEXT);
    }

    /**
     * Handle ephemeral typing indicators.
     * Destination: /app/conversations/{conversationId}/typing
     */
    @MessageMapping("/conversations/{conversationId}/typing")
    public void handleTyping(
            @DestinationVariable UUID conversationId,
            @Payload Map<String, Object> payload,
            Principal principal) {
        UserPrincipal userPrincipal = extractUserPrincipal(principal);
        if (userPrincipal == null) {
            return;
        }

        boolean typing = Boolean.TRUE.equals(payload.get("typing"));
        messageService.sendTypingEvent(conversationId, userPrincipal.getUserId(), typing);
    }

    private UserPrincipal extractUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof UserPrincipal up) {
                return up;
            }
        } else if (principal instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }
}

