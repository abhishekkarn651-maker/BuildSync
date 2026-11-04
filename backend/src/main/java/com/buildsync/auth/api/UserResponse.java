package com.buildsync.auth.api;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Safe user response payload excluding sensitive credentials and password hashes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String fullName;
    private String username;
    private String email;
    private List<String> roles;
    private Boolean emailVerified;

    public UserResponse() {
    }

    public UserResponse(UUID id, String fullName, String username, String email, List<String> roles, Boolean emailVerified) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.emailVerified = emailVerified;
    }

    public static UserResponse fromUser(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .collect(Collectors.toList());

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                roleNames,
                user.isEmailVerified()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}

