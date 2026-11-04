package com.buildsync.github.api.dto;

import java.time.Instant;

public record GitHubSyncResponse(
        int importedCount,
        int updatedCount,
        int totalCount,
        Instant syncedAt
) {
}

