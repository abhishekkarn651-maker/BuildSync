package com.buildsync.github.infrastructure;

import com.buildsync.github.domain.GitHubConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubConnectionRepository extends JpaRepository<GitHubConnection, UUID> {

    Optional<GitHubConnection> findByUserId(UUID userId);

    Optional<GitHubConnection> findByGithubUserId(Long githubUserId);

    boolean existsByGithubUserId(Long githubUserId);

    boolean existsByUserId(UUID userId);

    @Query("SELECT c FROM GitHubConnection c JOIN FETCH c.user u WHERE LOWER(u.username) = LOWER(:username)")
    Optional<GitHubConnection> findByBuildSyncUsername(@Param("username") String username);
}

