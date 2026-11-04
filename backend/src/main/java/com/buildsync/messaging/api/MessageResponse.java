package com.buildsync.messaging.api;

import com.buildsync.messaging.domain.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        MessageSenderSummary sender,
        String content,
        MessageType messageType,
        Instant createdAt,
        Instant updatedAt
) {}

