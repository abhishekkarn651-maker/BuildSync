package com.buildsync.github.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GitHubApiClientImpl implements GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClientImpl.class);
    private static final String GITHUB_API_VERSION = "2022-11-28";
    private static final String USER_AGENT = "BuildSync-Application";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String callbackUrl;
    private final String apiBaseUrl;
    private final String oauthBaseUrl;

    public GitHubApiClientImpl(
            @Value("${buildsync.github.app-client-id}") String clientId,
            @Value("${buildsync.github.app-client-secret}") String clientSecret,
            @Value("${buildsync.github.callback-url}") String callbackUrl,
            @Value("${buildsync.github.api-base-url:https://api.github.com}") String apiBaseUrl,
            @Value("${buildsync.github.oauth-base-url:https://github.com/login/oauth}") String oauthBaseUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUrl = callbackUrl;
        this.apiBaseUrl = apiBaseUrl;
        this.oauthBaseUrl = oauthBaseUrl;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(6));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public GitHubOAuthTokenResponse exchangeAuthorizationCode(String code, String codeVerifier) {
        log.debug("Exchanging GitHub authorization code for user access token with PKCE");

        Map<String, String> payload = new HashMap<>();
        payload.put("client_id", clientId);
        payload.put("client_secret", clientSecret);
        payload.put("code", code);
        payload.put("redirect_uri", callbackUrl);
        if (codeVerifier != null && !codeVerifier.isBlank()) {
            payload.put("code_verifier", codeVerifier);
        }

        try {
            return restClient.post()
                    .uri(oauthBaseUrl + "/access_token")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(GitHubOAuthTokenResponse.class);
        } catch (HttpClientErrorException e) {
            log.error("GitHub OAuth code exchange failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("GitHub OAuth authorization failed: " + e.getStatusText(), e);
        }
    }

    @Override
    public GitHubOAuthTokenResponse refreshAccessToken(String refreshToken) {
        log.debug("Refreshing GitHub user access token");

        Map<String, String> payload = new HashMap<>();
        payload.put("client_id", clientId);
        payload.put("client_secret", clientSecret);
        payload.put("grant_type", "refresh_token");
        payload.put("refresh_token", refreshToken);

        try {
            return restClient.post()
                    .uri(oauthBaseUrl + "/access_token")
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(GitHubOAuthTokenResponse.class);
        } catch (HttpClientErrorException e) {
            log.warn("GitHub token refresh failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("GitHub token refresh failed: " + e.getStatusText(), e);
        }
    }

    @Override
    public void revokeAuthorization(String accessToken) {
        log.debug("Revoking GitHub authorization grant for user");

        Map<String, String> payload = Map.of("access_token", accessToken);

        try {
            restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(apiBaseUrl + "/applications/" + clientId + "/grant")
                    .headers(headers -> {
                        headers.setBasicAuth(clientId, clientSecret);
                        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
                        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Revocation is best-effort on upstream GitHub; local credentials must still be deleted
            log.warn("Upstream GitHub authorization revocation completed with warning: {}", e.getMessage());
        }
    }

    @Override
    public GitHubUserResponse getUserProfile(String accessToken) {
        log.debug("Fetching GitHub authenticated user profile");

        return restClient.get()
                .uri(apiBaseUrl + "/user")
                .headers(headers -> {
                    headers.setBearerAuth(accessToken);
                    headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                    headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
                    headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
                })
                .retrieve()
                .body(GitHubUserResponse.class);
    }

    @Override
    public List<GitHubRepoDto> getUserRepositories(String accessToken, int page, int perPage) {
        log.debug("Fetching user repositories from GitHub (page {}, perPage {})", page, perPage);

        String uri = String.format("%s/user/repos?per_page=%d&page=%d&sort=updated&affiliation=owner,collaborator,organization_member",
                apiBaseUrl, perPage, page);

        try {
            List<GitHubRepoDto> repos = restClient.get()
                    .uri(uri)
                    .headers(headers -> {
                        headers.setBearerAuth(accessToken);
                        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
                        headers.set("X-GitHub-Api-Version", GITHUB_API_VERSION);
                        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubRepoDto>>() {});

            return repos != null ? repos : Collections.emptyList();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("GitHub access token is unauthorized or revoked");
            throw e;
        }
    }
}
