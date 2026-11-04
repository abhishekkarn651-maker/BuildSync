package com.buildsync.github.infrastructure;

import com.buildsync.github.domain.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, UUID>, JpaSpecificationExecutor<GitHubRepository> {

    Optional<GitHubRepository> findByConnectionIdAndGithubRepositoryId(UUID connectionId, Long githubRepositoryId);

    List<GitHubRepository> findByConnectionId(UUID connectionId);

    int countByConnectionId(UUID connectionId);

    @Query("SELECT r FROM GitHubRepository r JOIN r.connection c JOIN c.user u " +
           "WHERE LOWER(u.username) = LOWER(:username) AND r.isShowcased = true AND r.isPrivate = false " +
           "ORDER BY r.starsCount DESC, r.pushedAt DESC")
    List<GitHubRepository> findShowcasedPublicRepositoriesByUsername(@Param("username") String username);

    @Query("SELECT r FROM GitHubRepository r WHERE r.connection.id = :connectionId AND r.id = :repositoryId")
    Optional<GitHubRepository> findByConnectionIdAndId(@Param("connectionId") UUID connectionId, @Param("repositoryId") UUID repositoryId);
}

