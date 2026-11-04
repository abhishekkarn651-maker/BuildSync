package com.buildsync.github.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepoDto(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("description") String description,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("private") Boolean isPrivate,
        @JsonProperty("visibility") String visibility,
        @JsonProperty("default_branch") String defaultBranch,
        @JsonProperty("language") String language,
        @JsonProperty("stargazers_count") Integer starsCount,
        @JsonProperty("forks_count") Integer forksCount,
        @JsonProperty("open_issues_count") Integer openIssuesCount,
        @JsonProperty("pushed_at") Instant pushedAt,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt,
        @JsonProperty("owner") GitHubRepoOwnerDto owner
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GitHubRepoOwnerDto(
            @JsonProperty("login") String login,
            @JsonProperty("avatar_url") String avatarUrl
    ) {}
}

