package com.buildsync.messaging.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record MessageSenderSummary(
        UUID id,
        @JsonProperty("userId")
        UUID userId,
        String username,
        String fullName,
        String avatarUrl,
        String headline
) {
    public MessageSenderSummary(UUID id, String username, String fullName, String avatarUrl, String headline) {
        this(id, id, username, fullName, avatarUrl, headline);
    }
}


