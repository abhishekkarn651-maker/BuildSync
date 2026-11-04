package com.buildsync.messaging.api;

import java.time.Instant;
import java.util.UUID;

public record TypingEvent(
        UUID conversationId,
        UUID userId,
        String username,
        boolean typing,
        Instant timestamp
) {}

