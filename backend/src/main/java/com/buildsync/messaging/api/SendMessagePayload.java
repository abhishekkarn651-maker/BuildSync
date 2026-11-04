package com.buildsync.messaging.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessagePayload(
        @NotBlank(message = "Message content cannot be blank")
        @Size(max = 4000, message = "Message content cannot exceed 4000 characters")
        String content
) {}

