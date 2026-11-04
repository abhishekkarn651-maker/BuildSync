package com.buildsync.github.infrastructure;

import com.buildsync.github.domain.ProjectGitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectGitHubRepositoryRepository extends JpaRepository<ProjectGitHubRepository, UUID> {

    @Query("SELECT pgr FROM ProjectGitHubRepository pgr JOIN FETCH pgr.githubRepository r WHERE pgr.project.id = :projectId ORDER BY pgr.createdAt ASC")
    List<ProjectGitHubRepository> findByProjectIdWithRepository(@Param("projectId") UUID projectId);

    Optional<ProjectGitHubRepository> findByProjectIdAndGithubRepositoryId(UUID projectId, UUID githubRepositoryId);

    boolean existsByProjectIdAndGithubRepositoryId(UUID projectId, UUID githubRepositoryId);

    void deleteByProjectIdAndGithubRepositoryId(UUID projectId, UUID githubRepositoryId);
}

