package com.buildsync.github.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkProjectRepositoryRequest(
        @NotNull(message = "GitHub repository ID is required")
        UUID githubRepositoryId
) {
}

