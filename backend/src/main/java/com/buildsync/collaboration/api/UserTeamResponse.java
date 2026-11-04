package com.buildsync.collaboration.api;

import com.buildsync.project.api.ProjectSummaryResponse;

import java.time.Instant;

/**
 * Summary of a project team where the authenticated user is either the owner or an active member.
 */
public class UserTeamResponse {

    private ProjectSummaryResponse project;
    private String role; // "OWNER" or "MEMBER"
    private boolean isOwner;
    private long memberCount;
    private Instant joinedAt;

    public UserTeamResponse() {
    }

    public UserTeamResponse(ProjectSummaryResponse project, String role, boolean isOwner, long memberCount, Instant joinedAt) {
        this.project = project;
        this.role = role;
        this.isOwner = isOwner;
        this.memberCount = memberCount;
        this.joinedAt = joinedAt;
    }

    public ProjectSummaryResponse getProject() {
        return project;
    }

    public void setProject(ProjectSummaryResponse project) {
        this.project = project;
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

    public long getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(long memberCount) {
        this.memberCount = memberCount;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}

