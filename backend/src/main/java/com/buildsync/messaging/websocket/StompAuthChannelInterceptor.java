package com.buildsync.messaging.websocket;

import com.buildsync.messaging.application.ConversationService;
import com.buildsync.ratelimit.RateLimitExceededException;
import com.buildsync.ratelimit.RateLimitPolicy;
import com.buildsync.ratelimit.RateLimitResult;
import com.buildsync.ratelimit.RateLimitService;
import com.buildsync.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP Channel Interceptor performing JWT authentication on CONNECT frames
 * and dynamic participant authorization on SUBSCRIBE and SEND commands.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final Pattern CONVERSATION_DESTINATION_PATTERN = Pattern.compile("^/(?:topic|app)/conversations/([a-fA-F0-9\\-]+)(?:/.*)?$");

    private final JwtService jwtService;
    private final ConversationService conversationService;
    private final RateLimitService rateLimitService;

    public StompAuthChannelInterceptor(
            JwtService jwtService,
            @org.springframework.context.annotation.Lazy ConversationService conversationService,
            RateLimitService rateLimitService) {
        this.jwtService = jwtService;
        this.conversationService = conversationService;
        this.rateLimitService = rateLimitService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            accessor = StompHeaderAccessor.wrap(message);
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(message, accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            handleSend(accessor);
        }

        return message;
    }

    private Message<?> handleConnect(Message<?> message, StompHeaderAccessor accessor) {
        String token = extractBearerToken(accessor);
        if (token == null || token.isBlank()) {
            log.warn("STOMP CONNECT rejected: missing Authorization header");
            throw new IllegalArgumentException("STOMP CONNECT requires Authorization Bearer token");
        }

        try {
            Jwt jwt = jwtService.decode(token);
            UUID userId = jwtService.extractUserId(jwt);
            String username = jwt.getClaimAsString("username");

            UserPrincipal principal = new UserPrincipal(userId, username);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
            );

            if (accessor.isMutable()) {
                accessor.setUser(auth);
                log.info("STOMP CONNECT authenticated for user '{}' ({})", username, userId);
                return message;
            } else {
                StompHeaderAccessor mutableAccessor = StompHeaderAccessor.wrap(message);
                mutableAccessor.setUser(auth);
                log.info("STOMP CONNECT authenticated for user '{}' ({})", username, userId);
                return org.springframework.messaging.support.MessageBuilder.createMessage(
                        message.getPayload(), mutableAccessor.getMessageHeaders()
                );
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("STOMP CONNECT rejected: invalid or expired JWT: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid or expired JWT token for STOMP CONNECT", e);
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        Matcher matcher = CONVERSATION_DESTINATION_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String convIdStr = matcher.group(1);
            try {
                UUID conversationId = UUID.fromString(convIdStr);
                UserPrincipal principal = extractUserPrincipal(accessor);

                if (principal == null || !conversationService.hasAccess(conversationId, principal.getUserId())) {
                    log.warn("Forbidden STOMP SUBSCRIBE by '{}' to destination '{}'",
                            principal != null ? principal.getName() : "anonymous", destination);
                    throw new AccessDeniedException("You are not authorized to subscribe to conversation: " + convIdStr);
                }
            } catch (IllegalArgumentException e) {
                // Not a valid UUID, allow standard routing or let downstream handle
            }
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        Matcher matcher = CONVERSATION_DESTINATION_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String convIdStr = matcher.group(1);
            try {
                UUID conversationId = UUID.fromString(convIdStr);
                UserPrincipal principal = extractUserPrincipal(accessor);

                if (principal == null || !conversationService.hasAccess(conversationId, principal.getUserId())) {
                    log.warn("Forbidden STOMP SEND by '{}' to destination '{}'",
                            principal != null ? principal.getName() : "anonymous", destination);
                    throw new AccessDeniedException("You are not authorized to send to conversation: " + convIdStr);
                }

                // Enforce granular rate limits for sending messages and typing events
                if (destination.endsWith("/typing")) {
                    RateLimitResult result = rateLimitService.check(principal.getUserId().toString(), RateLimitPolicy.TYPING_EVENTS);
                    if (!result.allowed()) {
                        log.warn("STOMP typing event rejected: rate limit exceeded for user '{}' ({})",
                                principal.getName(), principal.getUserId());
                        throw new RateLimitExceededException("Rate limit exceeded for typing events", result, RateLimitPolicy.TYPING_EVENTS);
                    }
                } else if (destination.endsWith("/send")) {
                    RateLimitResult result = rateLimitService.check(principal.getUserId().toString(), RateLimitPolicy.DIRECT_MESSAGING);
                    if (!result.allowed()) {
                        log.warn("STOMP message rejected: rate limit exceeded for user '{}' ({})",
                                principal.getName(), principal.getUserId());
                        throw new RateLimitExceededException("Rate limit exceeded for sending messages. Please try again later.", result, RateLimitPolicy.DIRECT_MESSAGING);
                    }
                }
            } catch (IllegalArgumentException e) {
                // Not a valid UUID
            }
        }
    }

    private String extractBearerToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null) {
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
            return authHeader;
        }

        String passcode = accessor.getPasscode();
        if (passcode != null) {
            if (passcode.startsWith("Bearer ")) {
                return passcode.substring(7);
            }
            return passcode;
        }

        String customToken = accessor.getFirstNativeHeader("token");
        if (customToken != null) {
            if (customToken.startsWith("Bearer ")) {
                return customToken.substring(7);
            }
            return customToken;
        }

        return null;
    }

    private UserPrincipal extractUserPrincipal(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof UsernamePasswordAuthenticationToken auth) {
            if (auth.getPrincipal() instanceof UserPrincipal up) {
                return up;
            }
        }
        return null;
    }
}
