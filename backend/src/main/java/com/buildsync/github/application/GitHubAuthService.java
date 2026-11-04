package com.buildsync.github.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.github.api.dto.GitHubConnectInitResponse;
import com.buildsync.github.api.dto.GitHubConnectionStatusResponse;
import com.buildsync.github.client.GitHubApiClient;
import com.buildsync.github.client.GitHubOAuthTokenResponse;
import com.buildsync.github.client.GitHubUserResponse;
import com.buildsync.github.domain.GitHubConnection;
import com.buildsync.github.domain.GitHubConnectionStatus;
import com.buildsync.github.domain.GitHubOAuthState;
import com.buildsync.github.infrastructure.GitHubConnectionRepository;
import com.buildsync.github.infrastructure.GitHubOAuthStateRepository;
import com.buildsync.github.infrastructure.GitHubRepositoryRepository;
import com.buildsync.github.security.PkceUtil;
import com.buildsync.github.security.TokenEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class GitHubAuthService {

    private static final Logger log = LoggerFactory.getLogger(GitHubAuthService.class);
    private static final Duration STATE_EXPIRATION = Duration.ofMinutes(10);
    private static final long DEFAULT_ACCESS_TOKEN_LIFETIME_SECONDS = 28800L; // 8 hours
    private static final long DEFAULT_REFRESH_TOKEN_LIFETIME_SECONDS = 15811200L; // 6 months

    private final GitHubApiClient gitHubApiClient;
    private final TokenEncryptionService encryptionService;
    private final GitHubConnectionRepository connectionRepository;
    private final GitHubOAuthStateRepository oAuthStateRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final UserRepository userRepository;

    private final String clientId;
    private final String callbackUrl;
    private final String frontendRedirectUrl;
    private final String oauthBaseUrl;

    public GitHubAuthService(
            GitHubApiClient gitHubApiClient,
            TokenEncryptionService encryptionService,
            GitHubConnectionRepository connectionRepository,
            GitHubOAuthStateRepository oAuthStateRepository,
            GitHubRepositoryRepository repositoryRepository,
            UserRepository userRepository,
            @Value("${buildsync.github.app-client-id}") String clientId,
            @Value("${buildsync.github.callback-url}") String callbackUrl,
            @Value("${buildsync.github.frontend-redirect-url:http://localhost:3000/settings/integrations/github}") String frontendRedirectUrl,
            @Value("${buildsync.github.oauth-base-url:https://github.com/login/oauth}") String oauthBaseUrl) {
        this.gitHubApiClient = gitHubApiClient;
        this.encryptionService = encryptionService;
        this.connectionRepository = connectionRepository;
        this.oAuthStateRepository = oAuthStateRepository;
        this.repositoryRepository = repositoryRepository;
        this.userRepository = userRepository;
        this.clientId = clientId;
        this.callbackUrl = callbackUrl;
        this.frontendRedirectUrl = frontendRedirectUrl;
        this.oauthBaseUrl = oauthBaseUrl;
    }

    /**
     * Initiates GitHub App authorization flow using PKCE and secure server-side state.
     */
    @Transactional
    public GitHubConnectInitResponse initiateConnection(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        String rawState = PkceUtil.generateState();
        String stateHash = PkceUtil.hashState(rawState);
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = PkceUtil.computeCodeChallenge(codeVerifier);

        Instant expiresAt = Instant.now().plus(STATE_EXPIRATION);
        GitHubOAuthState stateRecord = new GitHubOAuthState(stateHash, codeVerifier, user, expiresAt);
        oAuthStateRepository.save(stateRecord);

        // Build authorization URL
        String authUrl = UriComponentsBuilder.fromUriString(oauthBaseUrl + "/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", callbackUrl)
                .queryParam("state", rawState)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode()
                .toUriString();

        log.info("Initiated GitHub OAuth connection flow for user '{}'", user.getUsername());
        return new GitHubConnectInitResponse(authUrl);
    }

    /**
     * Handles the public callback from GitHub App user authorization.
     * Returns the redirect URL for the frontend.
     */
    @Transactional
    public String handleOAuthCallback(String code, String rawState, String error, String errorDescription) {
        if (error != null && !error.isBlank()) {
            log.warn("GitHub OAuth authorization error returned: {} ({})", error, errorDescription);
            return buildFrontendRedirect("error", error);
        }

        if (code == null || code.isBlank() || rawState == null || rawState.isBlank()) {
            log.warn("Missing code or state parameter in GitHub OAuth callback");
            return buildFrontendRedirect("error", "missing_oauth_parameters");
        }

        String stateHash = PkceUtil.hashState(rawState);
        Optional<GitHubOAuthState> stateOpt = oAuthStateRepository.findByStateHash(stateHash);

        if (stateOpt.isEmpty()) {
            log.warn("State record not found in database for provided OAuth callback state");
            return buildFrontendRedirect("error", "invalid_state");
        }

        GitHubOAuthState stateRecord = stateOpt.get();

        if (stateRecord.isConsumed()) {
            log.warn("Attempt to replay previously consumed OAuth state for user '{}'", stateRecord.getUser().getId());
            return buildFrontendRedirect("error", "state_already_consumed");
        }

        if (stateRecord.isExpired()) {
            log.warn("Expired OAuth state submitted in callback for user '{}'", stateRecord.getUser().getId());
            return buildFrontendRedirect("error", "state_expired");
        }

        // Consume state immediately
        stateRecord.markConsumed();
        oAuthStateRepository.save(stateRecord);

        // Exchange authorization code for user access token with PKCE
        GitHubOAuthTokenResponse tokenResponse;
        try {
            tokenResponse = gitHubApiClient.exchangeAuthorizationCode(code, stateRecord.getCodeVerifier());
        } catch (Exception e) {
            log.error("Failed to exchange code with GitHub OAuth broker", e);
            return buildFrontendRedirect("error", "token_exchange_failed");
        }

        if (!tokenResponse.isSuccess()) {
            log.error("GitHub returned unsuccessful token exchange: {}", tokenResponse.errorDescription());
            return buildFrontendRedirect("error", tokenResponse.error() != null ? tokenResponse.error() : "token_exchange_rejected");
        }

        // Fetch user profile from GitHub
        GitHubUserResponse gitHubUser;
        try {
            gitHubUser = gitHubApiClient.getUserProfile(tokenResponse.accessToken());
        } catch (Exception e) {
            log.error("Failed to fetch user profile from GitHub with exchanged token", e);
            return buildFrontendRedirect("error", "profile_fetch_failed");
        }

        User user = stateRecord.getUser();

        // Enforce: Prevent a GitHub account from being linked to multiple BuildSync accounts
        Optional<GitHubConnection> existingWithGithubId = connectionRepository.findByGithubUserId(gitHubUser.id());
        if (existingWithGithubId.isPresent() && !existingWithGithubId.get().getUser().getId().equals(user.getId())) {
            log.warn("GitHub user ID {} is already linked to another BuildSync user {}", gitHubUser.id(), existingWithGithubId.get().getUser().getId());
            return buildFrontendRedirect("error", "github_account_already_linked");
        }

        // Encrypt credentials at rest
        String encryptedAccessToken = encryptionService.encrypt(tokenResponse.accessToken());
        String encryptedRefreshToken = encryptionService.encrypt(tokenResponse.refreshToken());

        Instant accessTokenExpiresAt = tokenResponse.expiresIn() != null
                ? Instant.now().plusSeconds(tokenResponse.expiresIn())
                : Instant.now().plusSeconds(DEFAULT_ACCESS_TOKEN_LIFETIME_SECONDS);

        Instant refreshTokenExpiresAt = tokenResponse.refreshTokenExpiresIn() != null
                ? Instant.now().plusSeconds(tokenResponse.refreshTokenExpiresIn())
                : Instant.now().plusSeconds(DEFAULT_REFRESH_TOKEN_LIFETIME_SECONDS);

        // Save or update connection
        GitHubConnection connection = connectionRepository.findByUserId(user.getId())
                .orElseGet(() -> new GitHubConnection(
                        user,
                        gitHubUser.id(),
                        gitHubUser.login(),
                        gitHubUser.avatarUrl(),
                        gitHubUser.htmlUrl(),
                        encryptedAccessToken,
                        encryptedRefreshToken,
                        accessTokenExpiresAt,
                        refreshTokenExpiresAt
                ));

        connection.setGithubUserId(gitHubUser.id());
        connection.setGithubUsername(gitHubUser.login());
        connection.setGithubAvatarUrl(gitHubUser.avatarUrl());
        connection.setGithubProfileUrl(gitHubUser.htmlUrl());
        connection.setEncryptedAccessToken(encryptedAccessToken);
        connection.setEncryptedRefreshToken(encryptedRefreshToken);
        connection.setAccessTokenExpiresAt(accessTokenExpiresAt);
        connection.setRefreshTokenExpiresAt(refreshTokenExpiresAt);
        connection.setConnectionStatus(GitHubConnectionStatus.CONNECTED);
        connection.setConnectedAt(Instant.now());

        connectionRepository.save(connection);

        // Async cleanup of old state records
        try {
            oAuthStateRepository.deleteExpiredOrConsumed(Instant.now().minus(Duration.ofHours(1)));
        } catch (Exception ignored) {
        }

        log.info("Successfully connected GitHub account '@{}' (ID: {}) for user '{}'",
                gitHubUser.login(), gitHubUser.id(), user.getUsername());

        return buildFrontendRedirect("success", gitHubUser.login());
    }

    /**
     * Retrieves current GitHub connection status and public statistics without exposing tokens.
     */
    @Transactional(readOnly = true)
    public GitHubConnectionStatusResponse getConnectionStatus(UUID userId) {
        Optional<GitHubConnection> connectionOpt = connectionRepository.findByUserId(userId);
        if (connectionOpt.isEmpty() || connectionOpt.get().getConnectionStatus() != GitHubConnectionStatus.CONNECTED) {
            return GitHubConnectionStatusResponse.disconnected();
        }

        GitHubConnection connection = connectionOpt.get();
        int repoCount = repositoryRepository.countByConnectionId(connection.getId());

        String tokenStatus = "VALID";
        if (connection.isRefreshTokenExpired()) {
            tokenStatus = "EXPIRED";
        }

        return new GitHubConnectionStatusResponse(
                true,
                connection.getGithubUsername(),
                connection.getGithubAvatarUrl(),
                connection.getGithubProfileUrl(),
                connection.getConnectedAt(),
                connection.getLastSyncedAt(),
                tokenStatus,
                repoCount
        );
    }

    /**
     * Safely disconnects the GitHub account, revoking tokens upstream and deleting credentials.
     */
    @Transactional
    public void disconnect(UUID userId) {
        Optional<GitHubConnection> connectionOpt = connectionRepository.findByUserId(userId);
        if (connectionOpt.isEmpty()) {
            return;
        }

        GitHubConnection connection = connectionOpt.get();

        // Best effort upstream revocation
        try {
            String decryptedToken = encryptionService.decrypt(connection.getEncryptedAccessToken());
            if (decryptedToken != null) {
                gitHubApiClient.revokeAuthorization(decryptedToken);
            }
        } catch (Exception e) {
            log.warn("Upstream revocation failed for user {}: {}", userId, e.getMessage());
        }

        // Delete connection and cascaded records
        connectionRepository.delete(connection);
        log.info("Disconnected and removed GitHub connection for user '{}'", userId);
    }

    /**
     * Resolves a valid decrypted access token, automatically refreshing and rotating tokens if expired.
     */
    @Transactional
    public synchronized String getValidAccessToken(GitHubConnection connection) {
        if (!connection.isAccessTokenExpired()) {
            return encryptionService.decrypt(connection.getEncryptedAccessToken());
        }

        log.info("GitHub access token expired for user '{}'. Initiating token refresh...", connection.getUser().getId());

        String decryptedRefreshToken = encryptionService.decrypt(connection.getEncryptedRefreshToken());
        if (decryptedRefreshToken == null || connection.isRefreshTokenExpired()) {
            connection.setConnectionStatus(GitHubConnectionStatus.REVOKED);
            connectionRepository.save(connection);
            throw new IllegalStateException("GitHub refresh token is missing or expired. Re-authentication required.");
        }

        GitHubOAuthTokenResponse refreshedToken = gitHubApiClient.refreshAccessToken(decryptedRefreshToken);
        if (!refreshedToken.isSuccess()) {
            connection.setConnectionStatus(GitHubConnectionStatus.REVOKED);
            connectionRepository.save(connection);
            throw new IllegalStateException("Failed to refresh GitHub access token. Upstream grant was revoked.");
        }

        String newEncryptedAccess = encryptionService.encrypt(refreshedToken.accessToken());
        String newEncryptedRefresh = refreshedToken.refreshToken() != null
                ? encryptionService.encrypt(refreshedToken.refreshToken())
                : connection.getEncryptedRefreshToken();

        Instant newAccessExpiry = refreshedToken.expiresIn() != null
                ? Instant.now().plusSeconds(refreshedToken.expiresIn())
                : Instant.now().plusSeconds(DEFAULT_ACCESS_TOKEN_LIFETIME_SECONDS);

        Instant newRefreshExpiry = refreshedToken.refreshTokenExpiresIn() != null
                ? Instant.now().plusSeconds(refreshedToken.refreshTokenExpiresIn())
                : connection.getRefreshTokenExpiresAt();

        connection.setEncryptedAccessToken(newEncryptedAccess);
        connection.setEncryptedRefreshToken(newEncryptedRefresh);
        connection.setAccessTokenExpiresAt(newAccessExpiry);
        connection.setRefreshTokenExpiresAt(newRefreshExpiry);
        connection.setConnectionStatus(GitHubConnectionStatus.CONNECTED);
        connectionRepository.save(connection);

        log.info("Successfully refreshed and rotated GitHub access token for user '{}'", connection.getUser().getId());
        return refreshedToken.accessToken();
    }

    private String buildFrontendRedirect(String key, String value) {
        String encoded = URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
        char separator = frontendRedirectUrl.contains("?") ? '&' : '?';
        if ("success".equals(key)) {
            return frontendRedirectUrl + separator + "status=success&github_user=" + encoded;
        }
        return frontendRedirectUrl + separator + "status=error&error=" + encoded;
    }
}

