package com.buildsync.collaboration.api;

import jakarta.validation.constraints.Size;

/**
 * Payload for requesting to join a public project.
 */
public class CreateJoinRequest {

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String message;

    public CreateJoinRequest() {
    }

    public CreateJoinRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

