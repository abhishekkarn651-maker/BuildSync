package com.buildsync.messaging.api;

import com.buildsync.messaging.domain.ConversationType;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummaryResponse(
        UUID id,
        ConversationType conversationType,
        ConversationProjectSummary project,
        MessageSenderSummary otherParticipant,
        MessagePreviewSummary lastMessage,
        long unreadCount,
        Instant createdAt,
        Instant updatedAt
) {}

