package com.buildsync.collaboration.domain;

/**
 * Type of collaboration request.
 * - JOIN_REQUEST: A developer requests to join a public project.
 * - INVITATION: A project owner invites a developer to their project.
 */
public enum CollaborationRequestType {
    JOIN_REQUEST,
    INVITATION
}

