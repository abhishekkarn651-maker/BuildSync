package com.buildsync.collaboration.api;

import java.util.UUID;

/**
 * Public summary of a user in a collaboration context.
 */
public class CollaborationUserSummary {

    private UUID id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String headline;

    public CollaborationUserSummary() {
    }

    public CollaborationUserSummary(UUID id, String username, String fullName, String avatarUrl, String headline) {
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

