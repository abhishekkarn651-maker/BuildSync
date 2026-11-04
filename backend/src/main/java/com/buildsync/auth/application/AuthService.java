package com.buildsync.auth.application;

import com.buildsync.auth.api.AuthResponse;
import com.buildsync.auth.api.LoginRequest;
import com.buildsync.auth.api.UserResponse;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.AuthenticationFailedException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.security.JwtService;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service managing user authentication, login credential verification, token rotation, and current user retrieval.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    /**
     * Authenticates a user with email and password, returning an access token and HttpOnly refresh cookie.
     */
    @Transactional
    public AuthResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("Account is disabled. Please contact support.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        TokenService.RefreshTokenResult refreshResult = tokenService.issueRefreshToken(user, request.isRememberMe());
        ResponseCookie refreshCookie = tokenService.createRefreshTokenCookie(
                refreshResult.getRawToken(), refreshResult.getExpiresInSeconds());

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                jwtService.getExpirationSeconds(),
                UserResponse.fromUser(user)
        );

        return new AuthResult(authResponse, refreshCookie);
    }

    /**
     * Rotates an active refresh token, issuing a new JWT access token and rotated refresh cookie.
     */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        TokenService.RefreshTokenResult rotateResult = tokenService.rotateRefreshToken(rawRefreshToken);
        User user = rotateResult.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        ResponseCookie newRefreshCookie = tokenService.createRefreshTokenCookie(
                rotateResult.getRawToken(), rotateResult.getExpiresInSeconds());

        AuthResponse authResponse = new AuthResponse(
                newAccessToken,
                jwtService.getExpirationSeconds(),
                UserResponse.fromUser(user)
        );

        return new AuthResult(authResponse, newRefreshCookie);
    }

    /**
     * Revokes the active refresh token and returns a cleared cookie.
     */
    @Transactional
    public ResponseCookie logout(String rawRefreshToken) {
        tokenService.revokeToken(rawRefreshToken);
        return tokenService.createCleanRefreshTokenCookie();
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return UserResponse.fromUser(user);
    }

    public static class AuthResult {
        private final AuthResponse authResponse;
        private final ResponseCookie refreshCookie;

        public AuthResult(AuthResponse authResponse, ResponseCookie refreshCookie) {
            this.authResponse = authResponse;
            this.refreshCookie = refreshCookie;
        }

        public AuthResponse getAuthResponse() {
            return authResponse;
        }

        public ResponseCookie getRefreshCookie() {
            return refreshCookie;
        }
    }
}

