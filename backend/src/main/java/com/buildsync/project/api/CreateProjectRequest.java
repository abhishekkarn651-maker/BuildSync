package com.buildsync.project.api;

import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectVisibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload for creating a new collaborative project.
 */
public class CreateProjectRequest {

    @NotBlank(message = "Project title is required")
    @Size(min = 3, max = 150, message = "Project title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Short description is required")
    @Size(min = 5, max = 300, message = "Short description must be between 5 and 300 characters")
    private String shortDescription;

    @Size(max = 5000, message = "Detailed description must not exceed 5000 characters")
    private String detailedDescription;

    @NotNull(message = "Project category is required")
    private ProjectCategory category;

    private ProjectStatus status = ProjectStatus.PLANNING;

    private ProjectVisibility visibility = ProjectVisibility.PUBLIC;

    private List<UUID> technologyIds = new ArrayList<>();

    @Pattern(regexp = "^(https?://.+)?$", message = "GitHub URL must be a valid HTTP or HTTPS address")
    @Size(max = 255, message = "GitHub URL must not exceed 255 characters")
    private String githubUrl;

    @Pattern(regexp = "^(https?://.+)?$", message = "Demo URL must be a valid HTTP or HTTPS address")
    @Size(max = 255, message = "Demo URL must not exceed 255 characters")
    private String demoUrl;

    private LocalDate startDate;

    private LocalDate completionDate;

    public CreateProjectRequest() {
    }

    @AssertTrue(message = "Completion date cannot be before start date")
    public boolean isDateRangeValid() {
        if (startDate == null || completionDate == null) {
            return true;
        }
        return !completionDate.isBefore(startDate);
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

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
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

    public List<UUID> getTechnologyIds() {
        return technologyIds;
    }

    public void setTechnologyIds(List<UUID> technologyIds) {
        this.technologyIds = technologyIds != null ? technologyIds : new ArrayList<>();
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
}

