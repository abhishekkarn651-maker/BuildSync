package com.buildsync.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * Resolves client IP addresses securely.
 * Strictly ignores X-Forwarded-For headers unless trusted proxy mode is explicitly enabled
 * to prevent IP spoofing attacks.
 */
@Component
public class ClientIpResolver {

    private final RateLimitProperties properties;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    /**
     * Resolves the client IP from an HttpServletRequest.
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (properties.isTrustedProxyEnabled()) {
            String forwardedHeader = request.getHeader("X-Forwarded-For");
            String clientIp = extractFirstIp(forwardedHeader);
            if (clientIp != null && !clientIp.isBlank()) {
                return clientIp;
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp.trim())) {
                return realIp.trim();
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "127.0.0.1";
    }

    /**
     * Resolves the client IP from a WebSocket ServerHttpRequest.
     */
    public String resolveClientIp(ServerHttpRequest request) {
        if (properties.isTrustedProxyEnabled()) {
            var headers = request.getHeaders();
            String forwardedHeader = headers.getFirst("X-Forwarded-For");
            String clientIp = extractFirstIp(forwardedHeader);
            if (clientIp != null && !clientIp.isBlank()) {
                return clientIp;
            }

            String realIp = headers.getFirst("X-Real-IP");
            if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp.trim())) {
                return realIp.trim();
            }
        }

        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "127.0.0.1";
    }

    private String extractFirstIp(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        String[] parts = headerValue.split(",");
        for (String part : parts) {
            String candidate = part.trim();
            if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}

