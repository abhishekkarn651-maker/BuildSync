package com.buildsync.collaboration.domain;

/**
 * Roles for team members in a collaborative project.
 * Note: Project owner is identified directly via the project aggregate root (Project.owner).
 */
public enum ProjectMemberRole {
    MEMBER,
    CONTRIBUTOR,
    MAINTAINER
}

