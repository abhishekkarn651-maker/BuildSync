package com.buildsync.messaging.api;

import java.util.UUID;

public record MessageSenderSummary(
        UUID id,
        String username,
        String fullName,
        String avatarUrl,
        String headline
) {}

