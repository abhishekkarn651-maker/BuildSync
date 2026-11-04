package com.buildsync.github.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.collaboration.infrastructure.ProjectMemberRepository;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.github.api.dto.ProjectGitHubRepositoryResponse;
import com.buildsync.github.domain.GitHubConnection;
import com.buildsync.github.domain.GitHubRepository;
import com.buildsync.github.domain.ProjectGitHubRepository;
import com.buildsync.github.infrastructure.GitHubConnectionRepository;
import com.buildsync.github.infrastructure.GitHubRepositoryRepository;
import com.buildsync.github.infrastructure.ProjectGitHubRepositoryRepository;
import com.buildsync.project.domain.Project;
import com.buildsync.project.infrastructure.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectGitHubLinkService {

    private static final Logger log = LoggerFactory.getLogger(ProjectGitHubLinkService.class);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final GitHubConnectionRepository connectionRepository;
    private final GitHubRepositoryRepository gitHubRepositoryRepository;
    private final ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository;
    private final UserRepository userRepository;

    public ProjectGitHubLinkService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            GitHubConnectionRepository connectionRepository,
            GitHubRepositoryRepository gitHubRepositoryRepository,
            ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository,
            UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.connectionRepository = connectionRepository;
        this.gitHubRepositoryRepository = gitHubRepositoryRepository;
        this.projectGitHubRepositoryRepository = projectGitHubRepositoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Links an accessible GitHub repository to a BuildSync project.
     * Enforces project ownership and repository access permissions.
     */
    @Transactional
    public ProjectGitHubRepositoryResponse linkRepository(UUID currentUserId, UUID projectId, UUID repositoryId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can link GitHub repositories");
        }

        GitHubConnection connection = connectionRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("GitHub connection not found for user: " + currentUserId));

        GitHubRepository repository = gitHubRepositoryRepository.findByConnectionIdAndId(connection.getId(), repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found in authenticated user's account: " + repositoryId));

        if (projectGitHubRepositoryRepository.existsByProjectIdAndGithubRepositoryId(projectId, repositoryId)) {
            throw new IllegalArgumentException("Repository is already linked to this project");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));

        ProjectGitHubRepository link = new ProjectGitHubRepository(project, repository, user);
        ProjectGitHubRepository saved = projectGitHubRepositoryRepository.save(link);

        log.info("Linked GitHub repository '{}' to project '{}' by owner '{}'",
                repository.getFullName(), project.getTitle(), currentUserId);

        return ProjectGitHubRepositoryResponse.fromEntity(saved);
    }

    /**
     * Lists all linked GitHub repositories for a project, enforcing privacy redaction.
     */
    @Transactional(readOnly = true)
    public List<ProjectGitHubRepositoryResponse> getProjectRepositories(UUID currentUserId, UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        boolean isAuthorized = false;
        if (currentUserId != null) {
            isAuthorized = project.isOwner(currentUserId)
                    || projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId);
        }

        List<ProjectGitHubRepository> links = projectGitHubRepositoryRepository.findByProjectIdWithRepository(projectId);

        final boolean hasPrivilegedAccess = isAuthorized;

        return links.stream()
                // Privacy rule: Public viewers cannot view private GitHub repositories linked to a project
                .filter(link -> hasPrivilegedAccess || !link.getGithubRepository().isPrivate())
                .map(ProjectGitHubRepositoryResponse::fromEntity)
                .toList();
    }

    /**
     * Unlinks a GitHub repository from a project.
     */
    @Transactional
    public void unlinkRepository(UUID currentUserId, UUID projectId, UUID repositoryId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("Only the project owner can unlink GitHub repositories");
        }

        projectGitHubRepositoryRepository.deleteByProjectIdAndGithubRepositoryId(projectId, repositoryId);
        log.info("Unlinked GitHub repository '{}' from project '{}' by owner '{}'", repositoryId, projectId, currentUserId);
    }
}

