package com.buildsync.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * HTTP filter enforcing granular rate limits on REST API endpoints
 * and attaching standard rate-limiting headers.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Pattern COLLABORATION_PROJECT_PATTERN = Pattern.compile("^/api/v1/projects/[^/]+/(?:join-requests|invitations)(?:/.*)?$");

    private final RateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(
            RateLimitService rateLimitService,
            ClientIpResolver clientIpResolver,
            RateLimitProperties properties,
            ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Bypass CORS preflight requests
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // Bypass system health checks, actuators and API documentation
        return path.equals("/api/v1/health")
                || path.startsWith("/actuator")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/v3/api-docs")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);

        RateLimitPolicy policy = determinePolicy(request, isAuthenticated);
        String clientKey = resolveClientKey(request, isAuthenticated, auth, policy);

        RateLimitResult result = rateLimitService.check(clientKey, policy);

        // Populate standard rate-limiting headers on all evaluated responses
        response.setHeader("RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("RateLimit-Reset", String.valueOf(result.resetSeconds()));

        if (result.allowed()) {
            filterChain.doFilter(request, response);
        } else {
            handleRateLimitExceeded(request, response, result);
        }
    }

    private RateLimitPolicy determinePolicy(HttpServletRequest request, boolean isAuthenticated) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 1. Authentication Endpoints
        if (HttpMethod.POST.matches(method) && path.equals("/api/v1/auth/login")) {
            return RateLimitPolicy.AUTH_LOGIN;
        }
        if (HttpMethod.POST.matches(method) && path.equals("/api/v1/auth/register")) {
            return RateLimitPolicy.AUTH_REGISTER;
        }
        if (HttpMethod.POST.matches(method) && path.equals("/api/v1/auth/refresh")) {
            return RateLimitPolicy.AUTH_REFRESH;
        }

        // 2. Avatar Upload Endpoint
        if (HttpMethod.POST.matches(method) && path.equals("/api/v1/profiles/me/avatar")) {
            return RateLimitPolicy.AVATAR_UPLOAD;
        }

        // 3. Search & Discoverability Endpoints
        if (HttpMethod.GET.matches(method) && path.equals("/api/v1/profiles")) {
            return RateLimitPolicy.SEARCH_DEVELOPERS;
        }
        if (HttpMethod.GET.matches(method) && path.equals("/api/v1/projects")) {
            return RateLimitPolicy.SEARCH_PROJECTS;
        }

        // 4. Collaboration & Team Management Endpoints
        if (path.startsWith("/api/v1/collaborations") || COLLABORATION_PROJECT_PATTERN.matcher(path).matches()) {
            return RateLimitPolicy.COLLABORATION_ACTIONS;
        }

        // 5. GitHub Integration Sync
        if (HttpMethod.POST.matches(method) && path.equals("/api/v1/integrations/github/sync")) {
            return RateLimitPolicy.GITHUB_SYNC;
        }

        // 6. General Fallbacks
        return isAuthenticated ? RateLimitPolicy.GENERAL_AUTHENTICATED : RateLimitPolicy.GENERAL_ANONYMOUS;
    }

    private String resolveClientKey(HttpServletRequest request, boolean isAuthenticated, Authentication auth, RateLimitPolicy policy) {
        // Strict IP-based policies
        if (policy == RateLimitPolicy.AUTH_LOGIN || policy == RateLimitPolicy.AUTH_REGISTER) {
            return clientIpResolver.resolveClientIp(request);
        }

        // Refresh token combines IP and identity or uses IP
        if (policy == RateLimitPolicy.AUTH_REFRESH) {
            String ip = clientIpResolver.resolveClientIp(request);
            if (isAuthenticated && auth != null) {
                return auth.getName() + ":" + ip;
            }
            return ip;
        }

        // User UUID for authenticated requests
        if (isAuthenticated && auth != null) {
            return auth.getName();
        }

        // Client IP for anonymous requests
        return clientIpResolver.resolveClientIp(request);
    }

    private void handleRateLimitExceeded(
            HttpServletRequest request,
            HttpServletResponse response,
            RateLimitResult result) throws IOException {

        response.setStatus(429); // 429 Too Many Requests
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("timestamp", LocalDateTime.now().format(ISO_FORMATTER));
        errorBody.put("status", 429);
        errorBody.put("error", "TOO_MANY_REQUESTS");
        errorBody.put("message", "Too many requests. Please try again later.");
        errorBody.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), errorBody);
    }
}

