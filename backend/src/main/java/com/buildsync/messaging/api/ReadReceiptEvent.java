package com.buildsync.messaging.api;

import java.time.Instant;
import java.util.UUID;

public record ReadReceiptEvent(
        UUID conversationId,
        UUID userId,
        String username,
        Instant readAt
) {}

