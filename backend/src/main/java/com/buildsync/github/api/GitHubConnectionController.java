package com.buildsync.github.api;

import com.buildsync.github.api.dto.GitHubConnectInitResponse;
import com.buildsync.github.api.dto.GitHubConnectionStatusResponse;
import com.buildsync.github.application.GitHubAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/github")
@Tag(name = "GitHub Connection Integration", description = "Endpoints for connecting, validating, and disconnecting GitHub App accounts")
public class GitHubConnectionController {

    private final GitHubAuthService gitHubAuthService;

    public GitHubConnectionController(GitHubAuthService gitHubAuthService) {
        this.gitHubAuthService = gitHubAuthService;
    }

    @PostMapping("/connect")
    @Operation(summary = "Start GitHub connection", description = "Creates a secure server-side PKCE state record and returns the GitHub OAuth authorization URL.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<GitHubConnectInitResponse> initiateConnect(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        GitHubConnectInitResponse response = gitHubAuthService.initiateConnection(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/callback")
    @Operation(summary = "GitHub OAuth callback", description = "Public redirect target from GitHub App authorization. Validates state, exchanges code for encrypted credentials, and redirects to frontend.")
    public RedirectView handleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        String redirectUrl = gitHubAuthService.handleOAuthCallback(code, state, error, errorDescription);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(redirectUrl);
        return redirectView;
    }

    @GetMapping("/status")
    @Operation(summary = "Get GitHub connection status", description = "Returns status of user's GitHub connection, public profile, and synchronization stats without exposing tokens.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<GitHubConnectionStatusResponse> getStatus(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        GitHubConnectionStatusResponse response = gitHubAuthService.getConnectionStatus(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/disconnect")
    @Operation(summary = "Disconnect GitHub account", description = "Revokes GitHub credentials, deletes local encrypted tokens, and cleans up synchronized records.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        gitHubAuthService.disconnect(userId);
        return ResponseEntity.noContent().build();
    }
}

