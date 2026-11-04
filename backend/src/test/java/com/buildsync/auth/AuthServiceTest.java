package com.buildsync.auth;

import com.buildsync.auth.api.LoginRequest;
import com.buildsync.auth.api.UserResponse;
import com.buildsync.auth.application.AuthService;
import com.buildsync.auth.application.TokenService;
import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.AuthenticationFailedException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    private AuthService authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, tokenService);

        testUser = new User("Jane Doe", "janedoe", "jane@example.com", "$2a$12$hashedPassword");
        testUser.setId(UUID.randomUUID());
        testUser.addRole(new Role(1L, RoleName.ROLE_USER.name()));
    }

    @Test
    @DisplayName("Should authenticate user and return access token with refresh cookie")
    void testSuccessfulLogin() {
        LoginRequest request = new LoginRequest("jane@example.com", "CorrectPassword123", true);

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("CorrectPassword123", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("valid-jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        TokenService.RefreshTokenResult refreshResult = new TokenService.RefreshTokenResult(
                "valid-refresh-token", 2592000L, testUser);
        when(tokenService.issueRefreshToken(any(User.class), anyBoolean())).thenReturn(refreshResult);
        when(tokenService.createRefreshTokenCookie(anyString(), anyLong()))
                .thenReturn(ResponseCookie.from("buildsync_refresh_token", "valid-refresh-token").build());

        AuthService.AuthResult result = authService.login(request);

        assertThat(result.getAuthResponse().getAccessToken()).isEqualTo("valid-jwt-token");
        assertThat(result.getAuthResponse().getUser().getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("Should reject login with incorrect password without revealing details")
    void testIncorrectPassword() {
        LoginRequest request = new LoginRequest("jane@example.com", "WrongPassword", false);

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should reject login with unknown email without revealing existence")
    void testUnknownEmail() {
        LoginRequest request = new LoginRequest("unknown@example.com", "SomePassword", false);

        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("Should reject login for disabled account")
    void testDisabledAccount() {
        testUser.setEnabled(false);
        LoginRequest request = new LoginRequest("jane@example.com", "CorrectPassword123", false);

        when(userRepository.findByEmailIgnoreCase("jane@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("CorrectPassword123", testUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("Account is disabled");
    }

    @Test
    @DisplayName("Should revoke refresh token and return empty cookie on logout")
    void testLogout() {
        when(tokenService.createCleanRefreshTokenCookie())
                .thenReturn(ResponseCookie.from("buildsync_refresh_token", "").maxAge(0).build());

        ResponseCookie cookie = authService.logout("raw-token-to-revoke");

        verify(tokenService).revokeToken("raw-token-to-revoke");
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    @Test
    @DisplayName("Should retrieve profile for existing user ID")
    void testGetCurrentUser() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        UserResponse response = authService.getCurrentUser(testUser.getId());

        assertThat(response.getId()).isEqualTo(testUser.getId());
        assertThat(response.getUsername()).isEqualTo("janedev".equals(testUser.getUsername()) ? "janedev" : "janedoe");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent user ID")
    void testGetCurrentUserNotFound() {
        UUID randomId = UUID.randomUUID();
        when(userRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser(randomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

