package com.buildsync.auth.api;

import com.buildsync.auth.application.AuthService;
import com.buildsync.auth.application.RegistrationService;
import com.buildsync.auth.application.TokenService;
import com.buildsync.common.exception.ErrorResponse;
import com.buildsync.common.exception.TokenRefreshException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing endpoints for registration, authentication, token rotation, and current user retrieval.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for developer account registration, JWT authentication, token rotation, and profile retrieval")
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthService authService;

    public AuthController(RegistrationService registrationService, AuthService authService) {
        this.registrationService = registrationService;
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new developer account", description = "Validates developer credentials, creates user with ROLE_USER, and returns JWT access token along with HttpOnly refresh cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request fields", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already in use", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegistrationService.RegistrationResult result = registrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString())
                .body(result.getAuthResponse());
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Verifies email and password using BCrypt, generates JWT access token, and sets persistent HttpOnly refresh token cookie.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid credentials format", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString())
                .body(result.getAuthResponse());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue new access token", description = "Validates the HttpOnly refresh token cookie from PostgreSQL, rotates the token within its family, and issues a new JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens successfully rotated", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token missing, expired, revoked, or reused", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = TokenService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new TokenRefreshException("Refresh token cookie is missing");
        }
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.getRefreshCookie().toString())
                .body(result.getAuthResponse());
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out user and revoke refresh token", description = "Revokes the active refresh token in PostgreSQL and clears the HttpOnly refresh cookie.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out successfully", content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT access token required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = TokenService.REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        ResponseCookie cleanCookie = authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(new MessageResponse("User logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Returns profile details and roles of the authenticated user extracted strictly from the JWT access token subject.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved", content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid JWT access token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UserResponse response = authService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }
}

