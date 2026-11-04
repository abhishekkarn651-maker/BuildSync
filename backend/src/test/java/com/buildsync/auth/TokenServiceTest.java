package com.buildsync.auth;

import com.buildsync.auth.application.TokenService;
import com.buildsync.auth.domain.RefreshToken;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.common.exception.TokenRefreshException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private TokenService tokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(refreshTokenRepository, false, "Lax", 7, 30);
        testUser = new User("John Doe", "johndoe", "john@example.com", "hashed_pwd");
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should issue cryptographically secure refresh token and store SHA-256 hash")
    void testIssueRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenService.RefreshTokenResult result = tokenService.issueRefreshToken(testUser, false);

        assertThat(result.getRawToken()).isNotBlank();
        assertThat(result.getExpiresInSeconds()).isEqualTo(7 * 24 * 3600);

        String computedHash = tokenService.hashToken(result.getRawToken());
        assertThat(computedHash).hasSize(64); // 64 hex characters for SHA-256
    }

    @Test
    @DisplayName("Should support extended lifespan for rememberMe session")
    void testIssueRememberMeRefreshToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TokenService.RefreshTokenResult result = tokenService.issueRefreshToken(testUser, true);

        assertThat(result.getExpiresInSeconds()).isEqualTo(30 * 24 * 3600);
    }

    @Test
    @DisplayName("Should rotate active token and mark previous token as replaced")
    void testRotateActiveToken() {
        String rawToken = "raw-active-refresh-token-value-123456789";
        String tokenHash = tokenService.hashToken(rawToken);
        UUID familyId = UUID.randomUUID();

        RefreshToken existingToken = new RefreshToken(
                testUser, tokenHash, familyId, Instant.now().plus(5, ChronoUnit.DAYS));
        existingToken.setId(UUID.randomUUID());

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
            }
            return token;
        });

        TokenService.RefreshTokenResult rotatedResult = tokenService.rotateRefreshToken(rawToken);

        assertThat(rotatedResult.getRawToken()).isNotEqualTo(rawToken);
        assertThat(existingToken.isRevoked()).isTrue();
        assertThat(existingToken.getReplacedByTokenId()).isNotNull();
    }

    @Test
    @DisplayName("Should detect refresh token reuse and revoke entire family")
    void testReuseDetectionRevokesFamily() {
        String rawToken = "raw-reused-token";
        String tokenHash = tokenService.hashToken(rawToken);
        UUID familyId = UUID.randomUUID();

        RefreshToken revokedToken = new RefreshToken(
                testUser, tokenHash, familyId, Instant.now().plus(5, ChronoUnit.DAYS));
        revokedToken.revoke(); // already revoked

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("reuse detected");

        verify(refreshTokenRepository).revokeFamily(eq(familyId), any(Instant.class));
    }

    @Test
    @DisplayName("Should reject expired refresh token")
    void testRejectExpiredRefreshToken() {
        String rawToken = "raw-expired-token";
        String tokenHash = tokenService.hashToken(rawToken);

        RefreshToken expiredToken = new RefreshToken(
                testUser, tokenHash, UUID.randomUUID(), Instant.now().minus(2, ChronoUnit.DAYS));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(rawToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should create clean HttpOnly cookie with maxAge 0 on logout")
    void testCreateCleanCookie() {
        ResponseCookie cleanCookie = tokenService.createCleanRefreshTokenCookie();

        assertThat(cleanCookie.getName()).isEqualTo(TokenService.REFRESH_COOKIE_NAME);
        assertThat(cleanCookie.getMaxAge().getSeconds()).isZero();
        assertThat(cleanCookie.isHttpOnly()).isTrue();
        assertThat(cleanCookie.getPath()).isEqualTo(TokenService.AUTH_COOKIE_PATH);
    }
}
