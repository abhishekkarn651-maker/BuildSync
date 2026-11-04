package com.buildsync.profile.api;

/**
 * Response payload containing the updated avatar reference.
 */
public class AvatarResponse {

    private String avatarUrl;
    private String message;

    public AvatarResponse() {
    }

    public AvatarResponse(String avatarUrl, String message) {
        this.avatarUrl = avatarUrl;
        this.message = message;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

