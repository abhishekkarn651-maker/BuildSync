package com.buildsync.auth.application;

import com.buildsync.auth.api.AuthResponse;
import com.buildsync.auth.api.RegisterRequest;
import com.buildsync.auth.api.UserResponse;
import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.DuplicateResourceException;
import com.buildsync.security.JwtService;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration, input normalization, duplicate prevention, and initial credential assignment.
 */
@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenService tokenService;

    public RegistrationService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenService tokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    /**
     * Registers a new developer account with normalized credentials and automatically assigns ROLE_USER.
     * Issues an initial access token and sets the refresh cookie for immediate session continuity.
     */
    @Transactional
    public RegistrationResult register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedUsername = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email address already exists.");
        }

        if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            throw new DuplicateResourceException("Username '" + normalizedUsername + "' is already taken.");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getFullName().trim(), normalizedUsername, normalizedEmail, encodedPassword);
        newUser.addRole(userRole);

        User savedUser = userRepository.save(newUser);

        // Generate JWT access token
        String accessToken = jwtService.generateAccessToken(savedUser);

        // Generate persistent refresh token
        TokenService.RefreshTokenResult refreshResult = tokenService.issueRefreshToken(savedUser, false);
        ResponseCookie refreshCookie = tokenService.createRefreshTokenCookie(
                refreshResult.getRawToken(), refreshResult.getExpiresInSeconds());

        AuthResponse authResponse = new AuthResponse(
                accessToken,
                jwtService.getExpirationSeconds(),
                UserResponse.fromUser(savedUser)
        );

        return new RegistrationResult(authResponse, refreshCookie);
    }

    public static class RegistrationResult {
        private final AuthResponse authResponse;
        private final ResponseCookie refreshCookie;

        public RegistrationResult(AuthResponse authResponse, ResponseCookie refreshCookie) {
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

