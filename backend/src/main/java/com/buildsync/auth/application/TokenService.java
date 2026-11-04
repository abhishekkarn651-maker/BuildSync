package com.buildsync.auth.application;

import com.buildsync.auth.domain.RefreshToken;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.common.exception.TokenRefreshException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service managing cryptographically secure opaque refresh tokens, SHA-256 token hashing,
 * token rotation, and reuse detection across token families.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    public static final String REFRESH_COOKIE_NAME = "buildsync_refresh_token";
    public static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean cookieSecure;
    private final String sameSite;
    private final long normalExpirationDays;
    private final long rememberMeExpirationDays;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${buildsync.security.cookie.secure:false}") boolean cookieSecure,
            @Value("${buildsync.security.cookie.same-site:Lax}") String sameSite,
            @Value("${buildsync.security.refresh-token.normal-days:7}") long normalExpirationDays,
            @Value("${buildsync.security.refresh-token.remember-me-days:30}") long rememberMeExpirationDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.cookieSecure = cookieSecure;
        this.sameSite = sameSite;
        this.normalExpirationDays = normalExpirationDays;
        this.rememberMeExpirationDays = rememberMeExpirationDays;
    }

    /**
     * Issues and persists a new cryptographically random refresh token.
     */
    @Transactional
    public RefreshTokenResult issueRefreshToken(User user, boolean rememberMe) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        long days = rememberMe ? rememberMeExpirationDays : normalExpirationDays;
        Duration duration = Duration.ofDays(days);
        Instant expiresAt = Instant.now().plus(duration);

        UUID familyId = UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken(user, tokenHash, familyId, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new RefreshTokenResult(rawToken, duration.getSeconds(), user);
    }

    /**
     * Rotates an active refresh token. If token reuse is detected, revokes the whole family.
     */
    @Transactional
    public RefreshTokenResult rotateRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            throw new TokenRefreshException("Refresh token is missing or empty");
        }

        String tokenHash = hashToken(rawRefreshToken.trim());
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenRefreshException("Invalid refresh token"));

        // Reuse detection: If token is already revoked, an adversary or stale client is attempting reuse
        if (existingToken.isRevoked()) {
            log.warn("Refresh token reuse detected for user ID {} on token family {}. Revoking entire token family.",
                    existingToken.getUser().getId(), existingToken.getFamilyId());
            refreshTokenRepository.revokeFamily(existingToken.getFamilyId(), Instant.now());
            throw new TokenRefreshException("Refresh token reuse detected. Session terminated for security. Please log in again.");
        }

        // Check if token has expired
        if (existingToken.isExpired()) {
            existingToken.revoke();
            refreshTokenRepository.save(existingToken);
            throw new TokenRefreshException("Refresh token has expired. Please log in again.");
        }

        // Check if user account is enabled
        User user = existingToken.getUser();
        if (!user.isEnabled()) {
            existingToken.revoke();
            refreshTokenRepository.save(existingToken);
            throw new TokenRefreshException("User account is disabled");
        }

        // Generate new rotated token in the same family
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        Duration duration = Duration.ofDays(normalExpirationDays);
        Instant newExpiresAt = Instant.now().plus(duration);

        RefreshToken newToken = new RefreshToken(user, newTokenHash, existingToken.getFamilyId(), newExpiresAt);
        refreshTokenRepository.save(newToken);

        // Mark previous token as replaced and revoked
        existingToken.revokeAndReplace(newToken.getId());
        refreshTokenRepository.save(existingToken);

        return new RefreshTokenResult(newRawToken, duration.getSeconds(), user);
    }

    /**
     * Revokes a refresh token during user logout.
     */
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.trim().isEmpty()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken.trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.isActive()) {
                token.revoke();
                refreshTokenRepository.save(token);
            }
        });
    }

    /**
     * Builds an HttpOnly, secure ResponseCookie containing the raw refresh token.
     */
    public ResponseCookie createRefreshTokenCookie(String rawRefreshToken, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();
    }

    /**
     * Builds an HttpOnly ResponseCookie that clears the refresh token in the browser.
     */
    public ResponseCookie createCleanRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(AUTH_COOKIE_PATH)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }

    /**
     * Hashes a raw token string using SHA-256 and returns a 64-character lowercase hexadecimal string.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in current runtime environment", e);
        }
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Holder for raw token string and calculated expiration seconds.
     */
    public static class RefreshTokenResult {
        private final String rawToken;
        private final long expiresInSeconds;
        private final User user;

        public RefreshTokenResult(String rawToken, long expiresInSeconds, User user) {
            this.rawToken = rawToken;
            this.expiresInSeconds = expiresInSeconds;
            this.user = user;
        }

        public String getRawToken() {
            return rawToken;
        }

        public long getExpiresInSeconds() {
            return expiresInSeconds;
        }

        public User getUser() {
            return user;
        }
    }
}

