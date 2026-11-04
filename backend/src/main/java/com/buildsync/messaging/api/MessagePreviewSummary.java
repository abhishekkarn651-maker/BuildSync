package com.buildsync.messaging.api;

import java.time.Instant;
import java.util.UUID;

public record MessagePreviewSummary(
        UUID id,
        String content,
        String senderUsername,
        Instant createdAt
) {}

