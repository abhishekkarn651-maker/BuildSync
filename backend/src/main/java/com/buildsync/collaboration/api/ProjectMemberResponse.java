package com.buildsync.collaboration.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Detailed representation of a project team member.
 */
public class ProjectMemberResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String headline;
    private String role;
    private boolean isOwner;
    private Instant joinedAt;

    public ProjectMemberResponse() {
    }

    public ProjectMemberResponse(UUID id,
                                 UUID userId,
                                 String username,
                                 String fullName,
                                 String avatarUrl,
                                 String headline,
                                 String role,
                                 boolean isOwner,
                                 Instant joinedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.headline = headline;
        this.role = role;
        this.isOwner = isOwner;
        this.joinedAt = joinedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}

