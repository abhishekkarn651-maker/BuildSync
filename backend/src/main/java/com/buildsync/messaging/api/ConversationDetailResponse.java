package com.buildsync.messaging.api;

import com.buildsync.messaging.domain.ConversationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ConversationDetailResponse(
        UUID id,
        ConversationType conversationType,
        ConversationProjectSummary project,
        List<ConversationParticipantSummary> participants,
        Instant createdAt,
        Instant updatedAt
) {
    @JsonProperty("type")
    public ConversationType type() {
        return conversationType;
    }
}


