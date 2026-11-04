package com.buildsync.messaging.api;

import java.time.Instant;
import java.util.UUID;

public record ConversationParticipantSummary(
        UUID id,
        UUID userId,
        String username,
        String fullName,
        String avatarUrl,
        Instant joinedAt,
        Instant lastReadAt
) {}

