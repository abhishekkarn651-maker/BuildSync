package com.buildsync.project.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Public representation of a project's creator/owner.
 * Strictly excludes private email addresses, passwords, and tokens.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectOwnerResponse {

    private UUID id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String headline;

    public ProjectOwnerResponse() {
    }

    public ProjectOwnerResponse(UUID id, String username, String fullName, String avatarUrl, String headline) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.headline = headline;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }
}

