package com.buildsync.github.api;

import com.buildsync.common.response.PagedResponse;
import com.buildsync.github.api.dto.GitHubRepositoryResponse;
import com.buildsync.github.api.dto.GitHubSyncResponse;
import com.buildsync.github.api.dto.UpdateRepoShowcaseRequest;
import com.buildsync.github.application.GitHubSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integrations/github")
@Tag(name = "GitHub Repository Integration", description = "Endpoints for synchronizing, filtering, and managing imported GitHub repositories")
public class GitHubRepositoryController {

    private final GitHubSyncService gitHubSyncService;

    public GitHubRepositoryController(GitHubSyncService gitHubSyncService) {
        this.gitHubSyncService = gitHubSyncService;
    }

    @PostMapping("/sync")
    @Operation(summary = "Synchronize repositories", description = "Fetches accessible repositories from GitHub REST API and updates local metadata.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<GitHubSyncResponse> syncRepositories(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        GitHubSyncResponse response = gitHubSyncService.syncRepositories(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories")
    @Operation(summary = "List imported repositories", description = "Returns a paginated list of imported repositories with filtering by search, language, privacy, and showcase status.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<PagedResponse<GitHubRepositoryResponse>> listRepositories(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "language", required = false) String language,
            @RequestParam(value = "private", required = false) Boolean isPrivate,
            @RequestParam(value = "showcased", required = false) Boolean isShowcased,
            @RequestParam(value = "sortBy", defaultValue = "stars") String sortBy,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());

        Sort sort = switch (sortBy.toLowerCase()) {
            case "pushed" -> Sort.by(Sort.Direction.DESC, "pushedAt");
            case "updated" -> Sort.by(Sort.Direction.DESC, "githubUpdatedAt");
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "starsCount");
        };

        Page<GitHubRepositoryResponse> result = gitHubSyncService.getRepositories(
                userId, search, language, isPrivate, isShowcased, PageRequest.of(page, size, sort));

        PagedResponse<GitHubRepositoryResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}")
    @Operation(summary = "Get repository detail", description = "Retrieves metadata for a specific imported repository.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<GitHubRepositoryResponse> getRepositoryDetail(
            @PathVariable UUID repositoryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        GitHubRepositoryResponse response = gitHubSyncService.getRepositoryDetail(userId, repositoryId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/repositories/{repositoryId}/showcase")
    @Operation(summary = "Toggle profile showcase", description = "Enables or disables showcasing a repository on the public developer profile. Private repositories cannot be showcased.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<GitHubRepositoryResponse> updateShowcaseStatus(
            @PathVariable UUID repositoryId,
            @Valid @RequestBody UpdateRepoShowcaseRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        GitHubRepositoryResponse response = gitHubSyncService.updateShowcaseStatus(userId, repositoryId, request.showcased());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/repositories/{repositoryId}")
    @Operation(summary = "Delete imported repository", description = "Removes an imported repository record from BuildSync.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> deleteRepository(
            @PathVariable UUID repositoryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        gitHubSyncService.deleteRepository(userId, repositoryId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/showcase/{username}")
    @Operation(summary = "Get public showcased repositories", description = "Public endpoint to retrieve developer's showcased public GitHub repositories for their profile.")
    public ResponseEntity<List<GitHubRepositoryResponse>> getPublicShowcase(@PathVariable String username) {
        List<GitHubRepositoryResponse> repositories = gitHubSyncService.getPublicShowcaseRepositories(username);
        return ResponseEntity.ok(repositories);
    }
}

