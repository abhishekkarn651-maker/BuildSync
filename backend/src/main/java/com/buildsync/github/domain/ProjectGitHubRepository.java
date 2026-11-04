package com.buildsync.github.domain;

import com.buildsync.auth.domain.User;
import com.buildsync.project.domain.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Join entity linking a BuildSync Project to an imported GitHub repository.
 */
@Entity
@Table(name = "project_github_repositories")
public class ProjectGitHubRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "github_repository_id", nullable = false)
    private GitHubRepository githubRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_by_user_id", nullable = false)
    private User linkedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ProjectGitHubRepository() {
    }

    public ProjectGitHubRepository(Project project, GitHubRepository githubRepository, User linkedByUser) {
        this.project = project;
        this.githubRepository = githubRepository;
        this.linkedByUser = linkedByUser;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public GitHubRepository getGithubRepository() {
        return githubRepository;
    }

    public void setGithubRepository(GitHubRepository githubRepository) {
        this.githubRepository = githubRepository;
    }

    public User getLinkedByUser() {
        return linkedByUser;
    }

    public void setLinkedByUser(User linkedByUser) {
        this.linkedByUser = linkedByUser;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

