package com.buildsync.project.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
import com.buildsync.project.api.CreateProjectRequest;
import com.buildsync.project.api.ProjectOwnerResponse;
import com.buildsync.project.api.ProjectResponse;
import com.buildsync.project.api.ProjectSummaryResponse;
import com.buildsync.project.api.ProjectTechnologyResponse;
import com.buildsync.project.api.UpdateProjectRequest;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectTechnology;
import com.buildsync.project.domain.ProjectVisibility;
import com.buildsync.project.infrastructure.ProjectRepository;
import com.buildsync.project.infrastructure.ProjectSpecifications;
import com.buildsync.project.infrastructure.ProjectTechnologyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing project creation, updates, ownership validation,
 * technology stack associations, and public discoverability.
 */
@Service
@Transactional
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectTechnologyRepository projectTechnologyRepository;
    private final UserRepository userRepository;
    private final DeveloperProfileRepository profileRepository;
    private final SkillRepository skillRepository;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectTechnologyRepository projectTechnologyRepository,
            UserRepository userRepository,
            DeveloperProfileRepository profileRepository,
            SkillRepository skillRepository) {
        this.projectRepository = projectRepository;
        this.projectTechnologyRepository = projectTechnologyRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.skillRepository = skillRepository;
    }

    /**
     * Create a new project for the authenticated developer.
     */
    @Transactional
    public ProjectResponse createProject(UUID ownerId, CreateProjectRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + ownerId));

        String slug = generateUniqueSlug(request.getTitle());

        Project project = new Project(
                owner,
                request.getTitle().trim(),
                slug,
                request.getShortDescription().trim(),
                request.getDetailedDescription() != null ? request.getDetailedDescription().trim() : null,
                request.getCategory(),
                request.getStatus() != null ? request.getStatus() : ProjectStatus.PLANNING,
                request.getVisibility() != null ? request.getVisibility() : ProjectVisibility.PUBLIC,
                request.getGithubUrl() != null && !request.getGithubUrl().trim().isEmpty() ? request.getGithubUrl().trim() : null,
                request.getDemoUrl() != null && !request.getDemoUrl().trim().isEmpty() ? request.getDemoUrl().trim() : null,
                request.getStartDate(),
                request.getCompletionDate()
        );

        Project savedProject = projectRepository.save(project);

        // Associate technologies
        List<ProjectTechnology> savedTechnologies = syncTechnologies(savedProject, request.getTechnologyIds());

        log.info("Created project '{}' ({}) owned by user '{}'",
                savedProject.getTitle(), savedProject.getId(), owner.getUsername());

        DeveloperProfile profile = profileRepository.findByUserId(ownerId).orElse(null);
        return mapToProjectResponse(savedProject, savedTechnologies, profile);
    }

    /**
     * Retrieve all projects owned by the authenticated developer.
     */
    @Transactional(readOnly = true)
    public List<ProjectSummaryResponse> getMyProjects(UUID ownerId) {
        List<Project> projects = projectRepository.findByOwnerIdWithOwnerOrderByUpdatedAtDesc(ownerId);
        if (projects.isEmpty()) {
            return Collections.emptyList();
        }

        DeveloperProfile profile = profileRepository.findByUserId(ownerId).orElse(null);

        List<UUID> projectIds = projects.stream().map(Project::getId).toList();
        List<ProjectTechnology> allTechnologies = !projectIds.isEmpty()
                ? projectTechnologyRepository.findByProjectIdInWithSkill(projectIds)
                : Collections.emptyList();
        Map<UUID, List<ProjectTechnology>> techMap = allTechnologies.stream()
                .collect(Collectors.groupingBy(pt -> pt.getProject().getId()));

        return projects.stream()
                .map(p -> mapToSummaryResponse(p, techMap.getOrDefault(p.getId(), Collections.emptyList()), profile))
                .toList();
    }

    /**
     * Retrieve a project by its UUID or unique slug.
     * Enforces privacy: private projects are strictly only viewable by their owner.
     */
    @Transactional(readOnly = true)
    public ProjectResponse getProjectByIdOrSlug(String projectIdOrSlug, UUID currentUserId) {
        Project project = resolveProject(projectIdOrSlug);

        // Visibility authorization
        if (project.getVisibility() == ProjectVisibility.PRIVATE) {
            if (currentUserId == null || !project.isOwner(currentUserId)) {
                // Return 404 to prevent resource enumeration
                throw new ResourceNotFoundException("Project not found with id or slug: " + projectIdOrSlug);
            }
        }

        List<ProjectTechnology> technologies = projectTechnologyRepository.findByProjectIdWithSkill(project.getId());
        DeveloperProfile profile = profileRepository.findByUserId(project.getOwner().getId()).orElse(null);

        return mapToProjectResponse(project, technologies, profile);
    }

    /**
     * Update an existing project. Only the project owner can update their project.
     */
    @Transactional
    public ProjectResponse updateProject(UUID projectId, UUID currentUserId, UpdateProjectRequest request) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to modify this project");
        }

        project.setTitle(request.getTitle().trim());
        project.setShortDescription(request.getShortDescription().trim());
        project.setDetailedDescription(request.getDetailedDescription() != null ? request.getDetailedDescription().trim() : null);
        project.setCategory(request.getCategory());
        project.setStatus(request.getStatus());
        project.setVisibility(request.getVisibility());
        project.setGithubUrl(request.getGithubUrl() != null && !request.getGithubUrl().trim().isEmpty() ? request.getGithubUrl().trim() : null);
        project.setDemoUrl(request.getDemoUrl() != null && !request.getDemoUrl().trim().isEmpty() ? request.getDemoUrl().trim() : null);
        project.setStartDate(request.getStartDate());
        project.setCompletionDate(request.getCompletionDate());

        Project updatedProject = projectRepository.save(project);

        // Update technologies
        projectTechnologyRepository.deleteByProjectId(projectId);
        projectTechnologyRepository.flush();
        List<ProjectTechnology> savedTechnologies = syncTechnologies(updatedProject, request.getTechnologyIds());

        log.info("Updated project '{}' ({}) by owner user id '{}'",
                updatedProject.getTitle(), updatedProject.getId(), currentUserId);

        DeveloperProfile profile = profileRepository.findByUserId(currentUserId).orElse(null);
        return mapToProjectResponse(updatedProject, savedTechnologies, profile);
    }

    /**
     * Delete an existing project. Only the project owner can delete their project.
     */
    @Transactional
    public void deleteProject(UUID projectId, UUID currentUserId) {
        Project project = projectRepository.findByIdWithOwner(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        if (!project.isOwner(currentUserId)) {
            throw new AccessDeniedException("You do not have permission to delete this project");
        }

        projectTechnologyRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);
        log.info("Deleted project '{}' ({}) by owner user id '{}'",
                project.getTitle(), project.getId(), currentUserId);
    }

    /**
     * Browse and search public projects with dynamic multi-criteria filtering, pagination, and sorting.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ProjectSummaryResponse> browsePublicProjects(
            String keyword,
            ProjectCategory category,
            ProjectStatus status,
            String technology,
            int page,
            int size,
            String sort) {

        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be negative");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50");
        }

        Sort resolvedSort = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, resolvedSort);

        Specification<Project> spec = ProjectSpecifications.publicProjectsWithFilters(
                keyword, category, status, technology);

        Page<Project> projectPage = projectRepository.findAll(spec, pageable);

        if (projectPage.isEmpty()) {
            return new PagedResponse<>(
                    Collections.emptyList(),
                    projectPage.getNumber(),
                    projectPage.getSize(),
                    projectPage.getTotalElements(),
                    projectPage.getTotalPages(),
                    projectPage.isLast()
            );
        }

        List<Project> projects = projectPage.getContent();
        List<UUID> projectIds = projects.stream().map(Project::getId).toList();

        // 1. Batch load technologies for all projects on current page
        List<ProjectTechnology> allTechnologies = !projectIds.isEmpty()
                ? projectTechnologyRepository.findByProjectIdInWithSkill(projectIds)
                : Collections.emptyList();
        Map<UUID, List<ProjectTechnology>> techMap = allTechnologies.stream()
                .collect(Collectors.groupingBy(pt -> pt.getProject().getId()));

        // 2. Batch load owner profiles for avatars/headlines
        Set<UUID> ownerUserIds = projects.stream()
                .map(p -> p.getOwner().getId())
                .collect(Collectors.toSet());
        List<DeveloperProfile> ownerProfiles = !ownerUserIds.isEmpty()
                ? profileRepository.findByUserIdInWithUser(ownerUserIds)
                : Collections.emptyList();
        Map<UUID, DeveloperProfile> profileMap = ownerProfiles.stream()
                .collect(Collectors.toMap(dp -> dp.getUser().getId(), dp -> dp));

        List<ProjectSummaryResponse> content = projects.stream()
                .map(p -> mapToSummaryResponse(
                        p,
                        techMap.getOrDefault(p.getId(), Collections.emptyList()),
                        profileMap.get(p.getOwner().getId())
                ))
                .toList();

        return new PagedResponse<>(
                content,
                projectPage.getNumber(),
                projectPage.getSize(),
                projectPage.getTotalElements(),
                projectPage.getTotalPages(),
                projectPage.isLast()
        );
    }

    /**
     * Retrieve technical skills associated with a project.
     */
    @Transactional(readOnly = true)
    public List<ProjectTechnologyResponse> getProjectTechnologies(String projectIdOrSlug, UUID currentUserId) {
        Project project = resolveProject(projectIdOrSlug);

        if (project.getVisibility() == ProjectVisibility.PRIVATE) {
            if (currentUserId == null || !project.isOwner(currentUserId)) {
                throw new ResourceNotFoundException("Project not found with id or slug: " + projectIdOrSlug);
            }
        }

        return projectTechnologyRepository.findByProjectIdWithSkill(project.getId()).stream()
                .map(pt -> ProjectTechnologyResponse.fromSkill(pt.getSkill()))
                .toList();
    }

    // Helper Methods

    private Project resolveProject(String projectIdOrSlug) {
        try {
            UUID id = UUID.fromString(projectIdOrSlug);
            return projectRepository.findByIdWithOwner(id)
                    .orElseGet(() -> projectRepository.findBySlugWithOwner(projectIdOrSlug)
                            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id or slug: " + projectIdOrSlug)));
        } catch (IllegalArgumentException notUuid) {
            return projectRepository.findBySlugWithOwner(projectIdOrSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("Project not found with slug: " + projectIdOrSlug));
        }
    }

    private List<ProjectTechnology> syncTechnologies(Project project, List<UUID> technologyIds) {
        if (technologyIds == null || technologyIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Deduplicate input technology IDs
        List<UUID> distinctIds = technologyIds.stream().distinct().toList();
        List<Skill> skills = skillRepository.findAllById(distinctIds);

        if (skills.size() != distinctIds.size()) {
            Set<UUID> foundIds = skills.stream().map(Skill::getId).collect(Collectors.toSet());
            List<UUID> missingIds = distinctIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException("The following technology skill IDs do not exist: " + missingIds);
        }

        List<ProjectTechnology> projectTechnologies = skills.stream()
                .map(skill -> new ProjectTechnology(project, skill))
                .toList();

        return projectTechnologyRepository.saveAll(projectTechnologies);
    }

    private String generateUniqueSlug(String title) {
        String base = title.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (base.isEmpty()) {
            base = "project";
        }

        String candidate = base;
        int attempts = 0;
        while (projectRepository.existsBySlug(candidate)) {
            attempts++;
            candidate = base + "-" + UUID.randomUUID().toString().substring(0, 6);
            if (attempts > 10) {
                candidate = base + "-" + System.currentTimeMillis();
                break;
            }
        }
        return candidate;
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        return switch (sort.toLowerCase().trim()) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "title_asc" -> Sort.by(Sort.Direction.ASC, "title");
            case "title_desc" -> Sort.by(Sort.Direction.DESC, "title");
            case "recently_updated" -> Sort.by(Sort.Direction.DESC, "updatedAt");
            default -> Sort.by(Sort.Direction.DESC, "updatedAt");
        };
    }

    private ProjectOwnerResponse mapOwner(User owner, DeveloperProfile profile) {
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
        String headline = profile != null ? profile.getHeadline() : null;
        return new ProjectOwnerResponse(
                owner.getId(),
                owner.getUsername(),
                owner.getFullName(),
                avatarUrl,
                headline
        );
    }

    private ProjectResponse mapToProjectResponse(
            Project project,
            List<ProjectTechnology> technologies,
            DeveloperProfile profile) {

        List<ProjectTechnologyResponse> techResponses = technologies != null
                ? technologies.stream().map(pt -> ProjectTechnologyResponse.fromSkill(pt.getSkill())).toList()
                : Collections.emptyList();

        return new ProjectResponse(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getShortDescription(),
                project.getDetailedDescription(),
                project.getCategory(),
                project.getStatus(),
                project.getVisibility(),
                project.getGithubUrl(),
                project.getDemoUrl(),
                project.getStartDate(),
                project.getCompletionDate(),
                mapOwner(project.getOwner(), profile),
                techResponses,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ProjectSummaryResponse mapToSummaryResponse(
            Project project,
            List<ProjectTechnology> technologies,
            DeveloperProfile profile) {

        List<ProjectTechnologyResponse> techResponses = technologies != null
                ? technologies.stream().map(pt -> ProjectTechnologyResponse.fromSkill(pt.getSkill())).toList()
                : Collections.emptyList();

        return new ProjectSummaryResponse(
                project.getId(),
                project.getSlug(),
                project.getTitle(),
                project.getShortDescription(),
                project.getCategory(),
                project.getStatus(),
                project.getVisibility(),
                project.getGithubUrl(),
                project.getDemoUrl(),
                project.getStartDate(),
                project.getCompletionDate(),
                mapOwner(project.getOwner(), profile),
                techResponses,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
