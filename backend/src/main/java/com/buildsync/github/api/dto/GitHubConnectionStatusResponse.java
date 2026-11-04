package com.buildsync.github.api.dto;

import java.time.Instant;

public record GitHubConnectionStatusResponse(
        boolean connected,
        String githubUsername,
        String githubAvatarUrl,
        String githubProfileUrl,
        Instant connectedAt,
        Instant lastSyncedAt,
        String tokenStatus,
        int importedRepositoryCount
) {
    public static GitHubConnectionStatusResponse disconnected() {
        return new GitHubConnectionStatusResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                "DISCONNECTED",
                0
        );
    }
}

