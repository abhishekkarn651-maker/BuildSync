package com.buildsync.auth;

import com.buildsync.auth.api.LoginRequest;
import com.buildsync.auth.api.RegisterRequest;
import com.buildsync.auth.application.TokenService;
import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.buildsync.auth.infrastructure.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.buildsync.profile.infrastructure.DeveloperProfileRepository developerProfileRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        developerProfileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        if (roleRepository.findByName(RoleName.ROLE_USER.name()).isEmpty()) {
            roleRepository.save(new Role(RoleName.ROLE_USER.name()));
        }
        if (roleRepository.findByName(RoleName.ROLE_ADMIN.name()).isEmpty()) {
            roleRepository.save(new Role(RoleName.ROLE_ADMIN.name()));
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register user and return JWT with HttpOnly cookie")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Abhishek Kumar", "abhishek", "abhishek@example.com", "SecurePassword123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.username").value("abhishek"))
                .andExpect(jsonPath("$.user.email").value("abhishek@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());

        // Verify password is encrypted in database
        User savedUser = userRepository.findByUsernameIgnoreCase("abhishek").orElseThrow();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("SecurePassword123");
        assertThat(passwordEncoder.matches("SecurePassword123", savedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject duplicate email with 409 Conflict")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest first = new RegisterRequest(
                "First User", "firstuser", "shared@example.com", "Password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest(
                "Second User", "seconduser", "shared@example.com", "Password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject duplicate username with 409 Conflict")
    void testRegisterDuplicateUsername() throws Exception {
        RegisterRequest first = new RegisterRequest(
                "First User", "sameusername", "user1@example.com", "Password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest(
                "Second User", "sameusername", "user2@example.com", "Password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject invalid email format with 400 Bad Request")
    void testRegisterInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "User Name", "validusername", "not-an-email", "Password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject short password (< 8 chars) with 400 Bad Request")
    void testRegisterShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "User Name", "validusername", "valid@example.com", "short");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should successfully authenticate and return tokens")
    void testLoginSuccess() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Login Test", "logintest", "logintest@example.com", "SecurePassword123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("logintest@example.com", "SecurePassword123", true);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("logintest"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject invalid password with 401 Unauthorized")
    void testLoginInvalidPassword() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "User", "userlogin", "userlogin@example.com", "CorrectPassword123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("userlogin@example.com", "WrongPassword!", false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject unknown email with 401 Unauthorized")
    void testLoginUnknownEmail() throws Exception {
        LoginRequest login = new LoginRequest("unknown@example.com", "Password123", false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Should reject unauthenticated access with 401 Unauthorized")
    void testMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Should return authenticated user profile when valid Bearer JWT is provided")
    void testMeWithValidToken() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Me User", "meuser", "meuser@example.com", "Password12345");
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = responseNode.get("accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("meuser"))
                .andExpect(jsonPath("$.email").value("meuser@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should rotate token and return new JWT")
    void testRefreshSuccess() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Refresh User", "refreshuser", "refresh@example.com", "Password12345");
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie(TokenService.REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("refreshuser"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should reject request when cookie is missing")
    void testRefreshMissingCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Should revoke token and clear cookie")
    void testLogoutSuccess() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "Logout User", "logoutuser", "logout@example.com", "Password12345");
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        String accessToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken").asText();
        Cookie refreshCookie = registerResult.getResponse().getCookie(TokenService.REFRESH_COOKIE_NAME);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.message").value("User logged out successfully"));
    }

    @Test
    @DisplayName("GET /api/v1/health - Remains public and UP")
    void testHealthRemainsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("BuildSync"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
