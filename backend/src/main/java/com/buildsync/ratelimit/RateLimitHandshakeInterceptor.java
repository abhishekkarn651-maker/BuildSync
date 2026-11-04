package com.buildsync.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Handshake interceptor enforcing WebSocket connection rate limits per IP.
 */
@Component
public class RateLimitHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitHandshakeInterceptor.class);

    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    public RateLimitHandshakeInterceptor(RateLimitService rateLimitService, ClientIpResolver clientIpResolver) {
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String clientIp = clientIpResolver.resolveClientIp(request);
        RateLimitResult result = rateLimitService.check(clientIp, RateLimitPolicy.WEBSOCKET_CONNECT);

        if (!result.allowed()) {
            log.warn("WebSocket handshake rejected for IP '{}': rate limit exceeded (retryAfter={}s)",
                    clientIp, result.retryAfterSeconds());
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().set("Retry-After", String.valueOf(result.retryAfterSeconds()));
            return false;
        }

        response.getHeaders().set("RateLimit-Limit", String.valueOf(result.limit()));
        response.getHeaders().set("RateLimit-Remaining", String.valueOf(result.remaining()));
        response.getHeaders().set("RateLimit-Reset", String.valueOf(result.resetSeconds()));

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op
    }
}

