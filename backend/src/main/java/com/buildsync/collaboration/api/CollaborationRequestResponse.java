package com.buildsync.collaboration.api;

import com.buildsync.collaboration.domain.CollaborationRequestStatus;
import com.buildsync.collaboration.domain.CollaborationRequestType;

import java.time.Instant;
import java.util.UUID;

/**
 * Full details of a collaboration request.
 */
public class CollaborationRequestResponse {

    private UUID id;
    private CollaborationProjectSummary project;
    private CollaborationUserSummary requester;
    private CollaborationUserSummary recipient;
    private CollaborationRequestType requestType;
    private CollaborationRequestStatus status;
    private String message;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant respondedAt;

    public CollaborationRequestResponse() {
    }

    public CollaborationRequestResponse(UUID id,
                                        CollaborationProjectSummary project,
                                        CollaborationUserSummary requester,
                                        CollaborationUserSummary recipient,
                                        CollaborationRequestType requestType,
                                        CollaborationRequestStatus status,
                                        String message,
                                        Instant createdAt,
                                        Instant updatedAt,
                                        Instant respondedAt) {
        this.id = id;
        this.project = project;
        this.requester = requester;
        this.recipient = recipient;
        this.requestType = requestType;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.respondedAt = respondedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CollaborationProjectSummary getProject() {
        return project;
    }

    public void setProject(CollaborationProjectSummary project) {
        this.project = project;
    }

    public CollaborationUserSummary getRequester() {
        return requester;
    }

    public void setRequester(CollaborationUserSummary requester) {
        this.requester = requester;
    }

    public CollaborationUserSummary getRecipient() {
        return recipient;
    }

    public void setRecipient(CollaborationUserSummary recipient) {
        this.recipient = recipient;
    }

    public CollaborationRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(CollaborationRequestType requestType) {
        this.requestType = requestType;
    }

    public CollaborationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(CollaborationRequestStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Instant respondedAt) {
        this.respondedAt = respondedAt;
    }
}

