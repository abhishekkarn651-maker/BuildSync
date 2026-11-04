package com.buildsync.github.api;

import com.buildsync.github.api.dto.LinkProjectRepositoryRequest;
import com.buildsync.github.api.dto.ProjectGitHubRepositoryResponse;
import com.buildsync.github.application.ProjectGitHubLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/github-repositories")
@Tag(name = "Project GitHub Linking", description = "Endpoints for linking and managing GitHub repositories associated with BuildSync projects")
public class ProjectGitHubRepositoryController {

    private final ProjectGitHubLinkService projectGitHubLinkService;

    public ProjectGitHubRepositoryController(ProjectGitHubLinkService projectGitHubLinkService) {
        this.projectGitHubLinkService = projectGitHubLinkService;
    }

    @PostMapping
    @Operation(summary = "Link GitHub repository to project", description = "Associates an imported GitHub repository with a project. Project owner only.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ProjectGitHubRepositoryResponse> linkRepository(
            @PathVariable UUID projectId,
            @Valid @RequestBody LinkProjectRepositoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        ProjectGitHubRepositoryResponse response = projectGitHubLinkService.linkRepository(
                currentUserId, projectId, request.githubRepositoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List project GitHub repositories", description = "Returns all linked GitHub repositories for a project. Private repositories are only visible to project members.")
    public ResponseEntity<List<ProjectGitHubRepositoryResponse>> getProjectRepositories(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        List<ProjectGitHubRepositoryResponse> response = projectGitHubLinkService.getProjectRepositories(currentUserId, projectId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{repositoryId}")
    @Operation(summary = "Unlink GitHub repository from project", description = "Removes the link between a GitHub repository and a project. Project owner only.")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> unlinkRepository(
            @PathVariable UUID projectId,
            @PathVariable UUID repositoryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        projectGitHubLinkService.unlinkRepository(currentUserId, projectId, repositoryId);
        return ResponseEntity.noContent().build();
    }
}

