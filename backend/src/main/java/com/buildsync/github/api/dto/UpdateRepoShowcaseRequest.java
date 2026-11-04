package com.buildsync.github.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRepoShowcaseRequest(
        @NotNull(message = "Showcase flag must be provided")
        Boolean showcased
) {
}

