package com.buildsync.messaging.api;

import jakarta.validation.constraints.NotBlank;

public record CreateDirectConversationRequest(
        @NotBlank(message = "Username is required")
        String username
) {}

