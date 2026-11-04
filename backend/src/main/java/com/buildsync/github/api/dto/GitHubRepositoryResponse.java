package com.buildsync.github.api.dto;

import com.buildsync.github.domain.GitHubRepository;
import com.buildsync.github.domain.RepositoryVisibility;

import java.time.Instant;
import java.util.UUID;

public record GitHubRepositoryResponse(
        UUID id,
        Long githubRepositoryId,
        String ownerLogin,
        String name,
        String fullName,
        String description,
        String htmlUrl,
        RepositoryVisibility visibility,
        boolean isPrivate,
        String defaultBranch,
        String primaryLanguage,
        int starsCount,
        int forksCount,
        int openIssuesCount,
        boolean isShowcased,
        Instant pushedAt,
        Instant githubCreatedAt,
        Instant githubUpdatedAt,
        Instant lastSyncedAt
) {
    public static GitHubRepositoryResponse fromEntity(GitHubRepository repo) {
        return new GitHubRepositoryResponse(
                repo.getId(),
                repo.getGithubRepositoryId(),
                repo.getOwnerLogin(),
                repo.getName(),
                repo.getFullName(),
                repo.getDescription(),
                repo.getHtmlUrl(),
                repo.getVisibility(),
                repo.isPrivate(),
                repo.getDefaultBranch(),
                repo.getPrimaryLanguage(),
                repo.getStarsCount(),
                repo.getForksCount(),
                repo.getOpenIssuesCount(),
                repo.isShowcased(),
                repo.getPushedAt(),
                repo.getGithubCreatedAt(),
                repo.getGithubUpdatedAt(),
                repo.getLastSyncedAt()
        );
    }
}

