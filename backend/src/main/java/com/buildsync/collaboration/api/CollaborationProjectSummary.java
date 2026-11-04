package com.buildsync.collaboration.api;

import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectVisibility;

import java.util.UUID;

/**
 * Summary of a project in a collaboration request.
 */
public class CollaborationProjectSummary {

    private UUID id;
    private String slug;
    private String title;
    private String shortDescription;
    private ProjectCategory category;
    private ProjectStatus status;
    private ProjectVisibility visibility;

    public CollaborationProjectSummary() {
    }

    public CollaborationProjectSummary(UUID id,
                                       String slug,
                                       String title,
                                       String shortDescription,
                                       ProjectCategory category,
                                       ProjectStatus status,
                                       ProjectVisibility visibility) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.shortDescription = shortDescription;
        this.category = category;
        this.status = status;
        this.visibility = visibility;
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
}

