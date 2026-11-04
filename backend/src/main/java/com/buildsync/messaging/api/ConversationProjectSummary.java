package com.buildsync.messaging.api;

import java.util.UUID;

public record ConversationProjectSummary(
        UUID id,
        String title,
        String slug,
        String visibility,
        String category
) {}

