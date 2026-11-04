package com.buildsync.github.application;

import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.github.api.dto.GitHubRepositoryResponse;
import com.buildsync.github.api.dto.GitHubSyncResponse;
import com.buildsync.github.client.GitHubApiClient;
import com.buildsync.github.client.GitHubRepoDto;
import com.buildsync.github.domain.GitHubConnection;
import com.buildsync.github.domain.GitHubConnectionStatus;
import com.buildsync.github.domain.GitHubRepository;
import com.buildsync.github.domain.RepositoryVisibility;
import com.buildsync.github.infrastructure.GitHubConnectionRepository;
import com.buildsync.github.infrastructure.GitHubRepositoryRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GitHubSyncService {

    private static final Logger log = LoggerFactory.getLogger(GitHubSyncService.class);
    private static final int PER_PAGE = 100;
    private static final int MAX_PAGES = 5; // Fetch up to 500 repositories per sync operation

    private final GitHubApiClient gitHubApiClient;
    private final GitHubAuthService gitHubAuthService;
    private final GitHubConnectionRepository connectionRepository;
    private final GitHubRepositoryRepository repositoryRepository;

    public GitHubSyncService(
            GitHubApiClient gitHubApiClient,
            GitHubAuthService gitHubAuthService,
            GitHubConnectionRepository connectionRepository,
            GitHubRepositoryRepository repositoryRepository) {
        this.gitHubApiClient = gitHubApiClient;
        this.gitHubAuthService = gitHubAuthService;
        this.connectionRepository = connectionRepository;
        this.repositoryRepository = repositoryRepository;
    }

    /**
     * Synchronizes accessible repositories from GitHub REST API for authenticated user.
     */
    @Transactional
    public GitHubSyncResponse syncRepositories(UUID userId) {
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found for user: " + userId));

        if (connection.getConnectionStatus() != GitHubConnectionStatus.CONNECTED) {
            throw new IllegalStateException("GitHub connection is inactive or revoked");
        }

        String accessToken = gitHubAuthService.getValidAccessToken(connection);

        int importedCount = 0;
        int updatedCount = 0;
        Instant now = Instant.now();

        for (int page = 1; page <= MAX_PAGES; page++) {
            List<GitHubRepoDto> pageRepos = gitHubApiClient.getUserRepositories(accessToken, page, PER_PAGE);
            if (pageRepos.isEmpty()) {
                break;
            }

            for (GitHubRepoDto dto : pageRepos) {
                if (dto.id() == null) continue;

                Optional<GitHubRepository> existingOpt = repositoryRepository
                        .findByConnectionIdAndGithubRepositoryId(connection.getId(), dto.id());

                String ownerLogin = dto.owner() != null && dto.owner().login() != null
                        ? dto.owner().login()
                        : connection.getGithubUsername();

                boolean isPriv = Boolean.TRUE.equals(dto.isPrivate());
                RepositoryVisibility visibility = isPriv ? RepositoryVisibility.PRIVATE : RepositoryVisibility.PUBLIC;
                if ("internal".equalsIgnoreCase(dto.visibility())) {
                    visibility = RepositoryVisibility.INTERNAL;
                }

                if (existingOpt.isPresent()) {
                    GitHubRepository existing = existingOpt.get();
                    existing.setName(dto.name() != null ? dto.name() : existing.getName());
                    existing.setFullName(dto.fullName() != null ? dto.fullName() : existing.getFullName());
                    existing.setOwnerLogin(ownerLogin);
                    existing.setDescription(dto.description());
                    existing.setHtmlUrl(dto.htmlUrl() != null ? dto.htmlUrl() : existing.getHtmlUrl());
                    existing.setPrivate(isPriv);
                    existing.setVisibility(visibility);
                    existing.setDefaultBranch(dto.defaultBranch() != null ? dto.defaultBranch() : "main");
                    existing.setPrimaryLanguage(dto.language());
                    existing.setStarsCount(dto.starsCount() != null ? dto.starsCount() : 0);
                    existing.setForksCount(dto.forksCount() != null ? dto.forksCount() : 0);
                    existing.setOpenIssuesCount(dto.openIssuesCount() != null ? dto.openIssuesCount() : 0);
                    existing.setPushedAt(dto.pushedAt());
                    existing.setGithubCreatedAt(dto.createdAt());
                    existing.setGithubUpdatedAt(dto.updatedAt());
                    existing.setLastSyncedAt(now);

                    // Privacy safeguard: if a repository transitioned from public to private, revoke showcased status
                    if (isPriv && existing.isShowcased()) {
                        existing.setShowcased(false);
                    }

                    repositoryRepository.save(existing);
                    updatedCount++;
                } else {
                    GitHubRepository newRepo = new GitHubRepository(
                            connection,
                            dto.id(),
                            ownerLogin,
                            dto.name() != null ? dto.name() : "repo",
                            dto.fullName() != null ? dto.fullName() : ownerLogin + "/repo",
                            dto.description(),
                            dto.htmlUrl() != null ? dto.htmlUrl() : "https://github.com/" + ownerLogin + "/repo",
                            visibility,
                            isPriv,
                            dto.defaultBranch() != null ? dto.defaultBranch() : "main",
                            dto.language(),
                            dto.starsCount() != null ? dto.starsCount() : 0,
                            dto.forksCount() != null ? dto.forksCount() : 0,
                            dto.openIssuesCount() != null ? dto.openIssuesCount() : 0,
                            dto.pushedAt(),
                            dto.createdAt(),
                            dto.updatedAt()
                    );
                    newRepo.setLastSyncedAt(now);
                    repositoryRepository.save(newRepo);
                    importedCount++;
                }
            }

            if (pageRepos.size() < PER_PAGE) {
                break;
            }
        }

        connection.setLastSyncedAt(now);
        connectionRepository.save(connection);

        int totalCount = repositoryRepository.countByConnectionId(connection.getId());
        log.info("Completed GitHub sync for user '{}': {} imported, {} updated, {} total",
                userId, importedCount, updatedCount, totalCount);

        return new GitHubSyncResponse(importedCount, updatedCount, totalCount, now);
    }

    /**
     * Lists imported repositories with dynamic search, language, privacy, and showcase filtering.
     */
    @Transactional(readOnly = true)
    public Page<GitHubRepositoryResponse> getRepositories(
            UUID userId,
            String search,
            String language,
            Boolean isPrivate,
            Boolean isShowcased,
            Pageable pageable) {

        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found for user: " + userId));

        Specification<GitHubRepository> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("connection").get("id"), connection.getId()));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                Predicate fullNameMatch = cb.like(cb.lower(root.get("fullName")), pattern);
                predicates.add(cb.or(nameMatch, descMatch, fullNameMatch));
            }

            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("primaryLanguage")), language.trim().toLowerCase()));
            }

            if (isPrivate != null) {
                predicates.add(cb.equal(root.get("isPrivate"), isPrivate));
            }

            if (isShowcased != null) {
                predicates.add(cb.equal(root.get("isShowcased"), isShowcased));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repositoryRepository.findAll(spec, pageable)
                .map(GitHubRepositoryResponse::fromEntity);
    }

    /**
     * Retrieves details of a specific imported repository.
     */
    @Transactional(readOnly = true)
    public GitHubRepositoryResponse getRepositoryDetail(UUID userId, UUID repositoryId) {
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found for user: " + userId));

        GitHubRepository repository = repositoryRepository.findByConnectionIdAndId(connection.getId(), repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repositoryId));

        return GitHubRepositoryResponse.fromEntity(repository);
    }

    /**
     * Updates showcase status for public profile display.
     */
    @Transactional
    public GitHubRepositoryResponse updateShowcaseStatus(UUID userId, UUID repositoryId, boolean showcased) {
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found for user: " + userId));

        GitHubRepository repository = repositoryRepository.findByConnectionIdAndId(connection.getId(), repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repositoryId));

        // Privacy rule: Private repositories must NEVER be showcased on public profile
        if (showcased && repository.isPrivate()) {
            throw new IllegalArgumentException("Private repositories cannot be showcased on public profiles");
        }

        repository.setShowcased(showcased);
        GitHubRepository saved = repositoryRepository.save(repository);
        log.info("Updated repository '{}' showcase status to {} for user '{}'", repository.getFullName(), showcased, userId);

        return GitHubRepositoryResponse.fromEntity(saved);
    }

    /**
     * Deletes an imported repository record.
     */
    @Transactional
    public void deleteRepository(UUID userId, UUID repositoryId) {
        GitHubConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found for user: " + userId));

        GitHubRepository repository = repositoryRepository.findByConnectionIdAndId(connection.getId(), repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repositoryId));

        repositoryRepository.delete(repository);
        log.info("Deleted imported repository '{}' for user '{}'", repositoryId, userId);
    }

    /**
     * Public showcase query for a developer's public profile.
     */
    @Transactional(readOnly = true)
    public List<GitHubRepositoryResponse> getPublicShowcaseRepositories(String username) {
        return repositoryRepository.findShowcasedPublicRepositoriesByUsername(username)
                .stream()
                .map(GitHubRepositoryResponse::fromEntity)
                .toList();
    }
}

