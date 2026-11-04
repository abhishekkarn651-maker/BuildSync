package com.buildsync.github.api.dto;

import com.buildsync.github.domain.ProjectGitHubRepository;

import java.time.Instant;
import java.util.UUID;

public record ProjectGitHubRepositoryResponse(
        UUID id,
        UUID projectId,
        GitHubRepositoryResponse repository,
        String linkedByUsername,
        Instant createdAt
) {
    public static ProjectGitHubRepositoryResponse fromEntity(ProjectGitHubRepository entity) {
        return new ProjectGitHubRepositoryResponse(
                entity.getId(),
                entity.getProject().getId(),
                GitHubRepositoryResponse.fromEntity(entity.getGithubRepository()),
                entity.getLinkedByUser().getUsername(),
                entity.getCreatedAt()
        );
    }
}

