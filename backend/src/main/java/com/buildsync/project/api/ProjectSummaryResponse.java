package com.buildsync.project.api;

import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectVisibility;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Summary representation of a project for discovery cards and dashboards.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectSummaryResponse {

    private UUID id;
    private String slug;
    private String title;
    private String shortDescription;
    private ProjectCategory category;
    private ProjectStatus status;
    private ProjectVisibility visibility;
    private String githubUrl;
    private String demoUrl;
    private LocalDate startDate;
    private LocalDate completionDate;
    private ProjectOwnerResponse owner;
    private List<ProjectTechnologyResponse> technologies;
    private Instant createdAt;
    private Instant updatedAt;

    public ProjectSummaryResponse() {
        this.technologies = new ArrayList<>();
    }

    public ProjectSummaryResponse(UUID id, String slug, String title, String shortDescription,
                                  ProjectCategory category, ProjectStatus status, ProjectVisibility visibility,
                                  String githubUrl, String demoUrl, LocalDate startDate, LocalDate completionDate,
                                  ProjectOwnerResponse owner, List<ProjectTechnologyResponse> technologies,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.shortDescription = shortDescription;
        this.category = category;
        this.status = status;
        this.visibility = visibility;
        this.githubUrl = githubUrl;
        this.demoUrl = demoUrl;
        this.startDate = startDate;
        this.completionDate = completionDate;
        this.owner = owner;
        this.technologies = technologies != null ? technologies : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public ProjectCategory getCategory() {
        return category;
    }

    public void setCategory(ProjectCategory category) {
        this.category = category;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public ProjectVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ProjectVisibility visibility) {
        this.visibility = visibility;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(LocalDate completionDate) {
        this.completionDate = completionDate;
    }

    public ProjectOwnerResponse getOwner() {
        return owner;
    }

    public void setOwner(ProjectOwnerResponse owner) {
        this.owner = owner;
    }

    public List<ProjectTechnologyResponse> getTechnologies() {
        return technologies;
    }

    public void setTechnologies(List<ProjectTechnologyResponse> technologies) {
        this.technologies = technologies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

