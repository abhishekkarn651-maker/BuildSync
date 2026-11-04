package com.buildsync.auth;

import com.buildsync.auth.api.RegisterRequest;
import com.buildsync.auth.application.RegistrationService;
import com.buildsync.auth.application.TokenService;
import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.DuplicateResourceException;
import com.buildsync.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                userRepository, roleRepository, passwordEncoder, jwtService, tokenService);
    }

    @Test
    @DisplayName("Should successfully register a new user with BCrypt password and ROLE_USER")
    void testSuccessfulRegistration() {
        RegisterRequest request = new RegisterRequest(
                "Abhishek Kumar", "Abhishek", "Abhishek@Example.COM", "SecurePassword123");

        when(userRepository.existsByEmailIgnoreCase("abhishek@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("abhishek")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER.name()))
                .thenReturn(Optional.of(new Role(1L, RoleName.ROLE_USER.name())));
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("$2a$12$hashedPasswordExample");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtService.generateAccessToken(any(User.class))).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(900L);

        TokenService.RefreshTokenResult refreshResult = new TokenService.RefreshTokenResult(
                "mock-refresh-token", 604800L, new User());
        when(tokenService.issueRefreshToken(any(User.class), anyBoolean())).thenReturn(refreshResult);
        when(tokenService.createRefreshTokenCookie(anyString(), anyLong()))
                .thenReturn(ResponseCookie.from("buildsync_refresh_token", "mock-refresh-token").build());

        RegistrationService.RegistrationResult result = registrationService.register(request);

        assertThat(result.getAuthResponse().getAccessToken()).isEqualTo("mock-jwt-token");
        assertThat(result.getAuthResponse().getUser().getUsername()).isEqualTo("abhishek");
        assertThat(result.getAuthResponse().getUser().getEmail()).isEqualTo("abhishek@example.com");
        assertThat(result.getAuthResponse().getUser().getRoles()).contains("ROLE_USER");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // Verify password hashing and normalization
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$12$hashedPasswordExample");
        assertThat(savedUser.getEmail()).isEqualTo("abhishek@example.com");
        assertThat(savedUser.getUsername()).isEqualTo("abhishek");
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void testRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Abhishek Kumar", "abhishek2", "existing@example.com", "SecurePassword123");

        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email address already exists");
    }

    @Test
    @DisplayName("Should reject duplicate username registration")
    void testRejectDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "Abhishek Kumar", "taken_user", "unique@example.com", "SecurePassword123");

        when(userRepository.existsByEmailIgnoreCase("unique@example.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("taken_user")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already taken");
    }
}

