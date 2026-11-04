package com.buildsync.github.client;

import java.util.List;

/**
 * Interface defining client communication with GitHub OAuth and REST API v3.
 */
public interface GitHubApiClient {

    /**
     * Exchanges an authorization code and PKCE code verifier for OAuth tokens.
     */
    GitHubOAuthTokenResponse exchangeAuthorizationCode(String code, String codeVerifier);

    /**
     * Refreshes an expired user access token using an expiring refresh token.
     */
    GitHubOAuthTokenResponse refreshAccessToken(String refreshToken);

    /**
     * Revokes user grant/authorization from GitHub App.
     */
    void revokeAuthorization(String accessToken);

    /**
     * Fetches authenticated user's GitHub profile.
     */
    GitHubUserResponse getUserProfile(String accessToken);

    /**
     * Fetches accessible repositories for the authenticated developer (paginated).
     */
    List<GitHubRepoDto> getUserRepositories(String accessToken, int page, int perPage);
}

