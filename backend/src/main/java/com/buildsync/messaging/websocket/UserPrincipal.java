package com.buildsync.messaging.websocket;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

/**
 * Principal representing an authenticated user across STOMP WebSocket sessions.
 */
public class UserPrincipal implements Principal {

    private final UUID userId;
    private final String username;

    public UserPrincipal(UUID userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public UUID getUserId() {
        return userId;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserPrincipal that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    @Override
    public String toString() {
        return "UserPrincipal{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }
}

