package com.buildsync.ratelimit;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.messaging.application.ConversationService;
import com.buildsync.messaging.websocket.StompAuthChannelInterceptor;
import com.buildsync.messaging.websocket.UserPrincipal;
import com.buildsync.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "buildsync.rate-limit.enabled=true")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private RateLimitHandshakeInterceptor handshakeInterceptor;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        rateLimitService.clear();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        String username = "rl_user_" + UUID.randomUUID().toString().substring(0, 8);
        testUser = new User("Rate Limit Test User", username, username + "@buildsync.test", passwordEncoder.encode("Password123!"));
        testUser.getRoles().add(userRole);
        testUser = userRepository.save(testUser);

        jwtToken = jwtService.generateAccessToken(testUser);
    }

    @AfterEach
    void tearDown() {
        rateLimitService.clear();
        if (testUser != null && testUser.getId() != null) {
            userRepository.deleteById(testUser.getId());
        }
    }

    @Test
    @DisplayName("Scenario 15: Public system health checks bypass rate limiting without quota headers")
    void testHealthEndpointBypassesRateLimiting() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.1");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("RateLimit-Limit"));
    }

    @Test
    @DisplayName("Scenario 16: CORS OPTIONS preflight requests bypass rate limiting immediately")
    void testCorsPreflightBypassesRateLimiting() throws Exception {
        mockMvc.perform(options("/api/v1/profiles")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.2");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, org.hamcrest.Matchers.containsString("RateLimit-Limit")));
    }

    @Test
    @DisplayName("Scenario 17: Anonymous request to general endpoints sets standard RateLimit headers")
    void testAnonymousGeneralEndpointHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.3");
                            return req;
                        }))
                .andExpect(header().string("RateLimit-Limit", "60"))
                .andExpect(header().exists("RateLimit-Remaining"))
                .andExpect(header().exists("RateLimit-Reset"));
    }

    @Test
    @DisplayName("Scenario 18 & 19: Login endpoint enforces 10 req/5 min/IP limit and returns HTTP 429 with standard body")
    void testLoginEndpointRateLimiting() throws Exception {
        String clientIp = "198.51.100.11";
        Map<String, String> badLoginPayload = Map.of(
                "usernameOrEmail", "nonexistent_user",
                "password", "WrongPassword123!"
        );
        String bodyJson = objectMapper.writeValueAsString(badLoginPayload);

        // Perform 10 login attempts (allowed up to capacity)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson)
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            }))
                    .andExpect(header().string("RateLimit-Limit", "10"))
                    .andExpect(header().string("RateLimit-Remaining", String.valueOf(9 - i)));
        }

        // 11th login attempt must be rejected with HTTP 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson)
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("RateLimit-Limit", "10"))
                .andExpect(header().string("RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.message").value("Too many requests. Please try again later."))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    @DisplayName("Scenario 20: Registration endpoint enforces 5 req/hour/IP limit and rejects 6th attempt")
    void testRegisterEndpointRateLimiting() throws Exception {
        String clientIp = "198.51.100.22";
        Map<String, String> invalidRegisterPayload = Map.of(
                "username", "",
                "email", "invalid-email",
                "password", "short"
        );
        String bodyJson = objectMapper.writeValueAsString(invalidRegisterPayload);

        // Consume all 5 registration quota tokens
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyJson)
                            .with(req -> {
                                req.setRemoteAddr(clientIp);
                                return req;
                            }))
                    .andExpect(header().string("RateLimit-Limit", "5"))
                    .andExpect(header().string("RateLimit-Remaining", String.valueOf(4 - i)));
        }

        // 6th attempt must be rejected with HTTP 429
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyJson)
                        .with(req -> {
                            req.setRemoteAddr(clientIp);
                            return req;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("TOO_MANY_REQUESTS"));
    }

    @Test
    @DisplayName("Scenario 21: Developer search endpoint enforces 60 req/min limit")
    void testDeveloperSearchEndpointRateLimiting() throws Exception {
        mockMvc.perform(get("/api/v1/profiles")
                        .param("q", "fullstack")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.33");
                            return req;
                        }))
                .andExpect(header().string("RateLimit-Limit", "60"));
    }

    @Test
    @DisplayName("Scenario 22: Project search endpoint enforces 60 req/min limit")
    void testProjectSearchEndpointRateLimiting() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .param("query", "react")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.44");
                            return req;
                        }))
                .andExpect(header().string("RateLimit-Limit", "60"));
    }

    @Test
    @DisplayName("Scenario 23: Authenticated request enforces GENERAL_AUTHENTICATED limit (120 req/min/user)")
    void testAuthenticatedGeneralEndpointLimit() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.55");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().string("RateLimit-Limit", "120"));
    }

    @Test
    @DisplayName("Scenario 24: Avatar upload endpoint enforces 10 uploads/hour/user quota")
    void testAvatarUploadRateLimitingPolicy() throws Exception {
        mockMvc.perform(post("/api/v1/profiles/me/avatar")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.66");
                            return req;
                        }))
                .andExpect(header().string("RateLimit-Limit", "10"));
    }

    @Test
    @DisplayName("Scenario 25: WebSocket handshake interceptor rejects connection attempts exceeding WEBSOCKET_CONNECT limit (10 attempts/min/IP)")
    void testWebSocketHandshakeInterceptorRateLimiting() {
        String clientIp = "198.51.100.77";

        org.springframework.http.server.ServletServerHttpRequest servletRequest =
                new org.springframework.http.server.ServletServerHttpRequest(new org.springframework.mock.web.MockHttpServletRequest() {{
                    setRemoteAddr(clientIp);
                }});
        org.springframework.http.server.ServletServerHttpResponse servletResponse =
                new org.springframework.http.server.ServletServerHttpResponse(new org.springframework.mock.web.MockHttpServletResponse());

        for (int i = 0; i < 10; i++) {
            boolean allowed = handshakeInterceptor.beforeHandshake(servletRequest, servletResponse, null, Collections.emptyMap());
            assertThat(allowed).isTrue();
        }

        // 11th attempt must be rejected
        boolean rejected = handshakeInterceptor.beforeHandshake(servletRequest, servletResponse, null, Collections.emptyMap());
        assertThat(rejected).isFalse();
        assertThat(servletResponse.getHeaders().getFirst("Retry-After")).isNotNull();
    }

    @Test
    @DisplayName("Scenario 26: STOMP chat message interceptor blocks message sending when user exceeds messaging quota")
    void testStompChatMessageInterceptorRateLimiting() {
        ConversationService convService = mock(ConversationService.class);
        UUID conversationId = UUID.randomUUID();
        when(convService.hasAccess(conversationId, testUser.getId())).thenReturn(true);

        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtService, convService, rateLimitService);
        MessageChannel channel = mock(MessageChannel.class);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/conversations/" + conversationId + "/send");
        UserPrincipal principal = new UserPrincipal(testUser.getId(), testUser.getUsername());
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 30 messages allowed
        for (int i = 0; i < 30; i++) {
            Message<?> result = interceptor.preSend(message, channel);
            assertThat(result).isNotNull();
        }

        // 31st message must throw RateLimitExceededException
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded for sending messages");
    }

    @Test
    @DisplayName("Scenario 27: STOMP typing event interceptor blocks events when user exceeds typing quota (60 events/min/user)")
    void testStompTypingInterceptorRateLimiting() {
        ConversationService convService = mock(ConversationService.class);
        UUID conversationId = UUID.randomUUID();
        when(convService.hasAccess(conversationId, testUser.getId())).thenReturn(true);

        StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtService, convService, rateLimitService);
        MessageChannel channel = mock(MessageChannel.class);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/conversations/" + conversationId + "/typing");
        UserPrincipal principal = new UserPrincipal(testUser.getId(), testUser.getUsername());
        accessor.setUser(new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList()));
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // 60 typing events allowed
        for (int i = 0; i < 60; i++) {
            Message<?> result = interceptor.preSend(message, channel);
            assertThat(result).isNotNull();
        }

        // 61st typing event must throw RateLimitExceededException
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded for typing events");
    }

    @Test
    @DisplayName("Scenario 28: Collaboration actions enforce COLLABORATION_ACTIONS limit (20 req/min/user)")
    void testCollaborationActionsRateLimiting() throws Exception {
        mockMvc.perform(get("/api/v1/collaborations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.88");
                            return req;
                        }))
                .andExpect(header().string("RateLimit-Limit", "20"));
    }
}
