package com.buildsync.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing an imported GitHub repository.
 */
@Entity
@Table(name = "github_repositories")
public class GitHubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_connection_id", nullable = false)
    private GitHubConnection connection;

    @Column(name = "github_repository_id", nullable = false)
    private Long githubRepositoryId;

    @Column(name = "owner_login", nullable = false, length = 100)
    private String ownerLogin;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "html_url", nullable = false, length = 255)
    private String htmlUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    private RepositoryVisibility visibility = RepositoryVisibility.PUBLIC;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch = "main";

    @Column(name = "primary_language", length = 50)
    private String primaryLanguage;

    @Column(name = "stars_count", nullable = false)
    private int starsCount = 0;

    @Column(name = "forks_count", nullable = false)
    private int forksCount = 0;

    @Column(name = "open_issues_count", nullable = false)
    private int openIssuesCount = 0;

    @Column(name = "is_showcased", nullable = false)
    private boolean isShowcased = false;

    @Column(name = "pushed_at")
    private Instant pushedAt;

    @Column(name = "github_created_at")
    private Instant githubCreatedAt;

    @Column(name = "github_updated_at")
    private Instant githubUpdatedAt;

    @Column(name = "last_synced_at", nullable = false)
    private Instant lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public GitHubRepository() {
    }

    public GitHubRepository(GitHubConnection connection,
                            Long githubRepositoryId,
                            String ownerLogin,
                            String name,
                            String fullName,
                            String description,
                            String htmlUrl,
                            RepositoryVisibility visibility,
                            boolean isPrivate,
                            String defaultBranch,
                            String primaryLanguage,
                            int starsCount,
                            int forksCount,
                            int openIssuesCount,
                            Instant pushedAt,
                            Instant githubCreatedAt,
                            Instant githubUpdatedAt) {
        this.connection = connection;
        this.githubRepositoryId = githubRepositoryId;
        this.ownerLogin = ownerLogin;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.htmlUrl = htmlUrl;
        this.visibility = visibility;
        this.isPrivate = isPrivate;
        this.defaultBranch = defaultBranch != null ? defaultBranch : "main";
        this.primaryLanguage = primaryLanguage;
        this.starsCount = starsCount;
        this.forksCount = forksCount;
        this.openIssuesCount = openIssuesCount;
        this.pushedAt = pushedAt;
        this.githubCreatedAt = githubCreatedAt;
        this.githubUpdatedAt = githubUpdatedAt;
        this.lastSyncedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.lastSyncedAt == null) {
            this.lastSyncedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GitHubConnection getConnection() {
        return connection;
    }

    public void setConnection(GitHubConnection connection) {
        this.connection = connection;
    }

    public Long getGithubRepositoryId() {
        return githubRepositoryId;
    }

    public void setGithubRepositoryId(Long githubRepositoryId) {
        this.githubRepositoryId = githubRepositoryId;
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public void setOwnerLogin(String ownerLogin) {
        this.ownerLogin = ownerLogin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public RepositoryVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(RepositoryVisibility visibility) {
        this.visibility = visibility;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public String getPrimaryLanguage() {
        return primaryLanguage;
    }

    public void setPrimaryLanguage(String primaryLanguage) {
        this.primaryLanguage = primaryLanguage;
    }

    public int getStarsCount() {
        return starsCount;
    }

    public void setStarsCount(int starsCount) {
        this.starsCount = starsCount;
    }

    public int getForksCount() {
        return forksCount;
    }

    public void setForksCount(int forksCount) {
        this.forksCount = forksCount;
    }

    public int getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(int openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public boolean isShowcased() {
        return isShowcased;
    }

    public void setShowcased(boolean showcased) {
        isShowcased = showcased;
    }

    public Instant getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(Instant pushedAt) {
        this.pushedAt = pushedAt;
    }

    public Instant getGithubCreatedAt() {
        return githubCreatedAt;
    }

    public void setGithubCreatedAt(Instant githubCreatedAt) {
        this.githubCreatedAt = githubCreatedAt;
    }

    public Instant getGithubUpdatedAt() {
        return githubUpdatedAt;
    }

    public void setGithubUpdatedAt(Instant githubUpdatedAt) {
        this.githubUpdatedAt = githubUpdatedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
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
}

