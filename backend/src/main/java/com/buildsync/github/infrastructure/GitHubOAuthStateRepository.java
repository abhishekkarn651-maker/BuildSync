package com.buildsync.github.infrastructure;

import com.buildsync.github.domain.GitHubOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GitHubOAuthStateRepository extends JpaRepository<GitHubOAuthState, UUID> {

    Optional<GitHubOAuthState> findByStateHash(String stateHash);

    @Modifying
    @Query("DELETE FROM GitHubOAuthState s WHERE s.expiresAt < :now OR (s.consumed = true AND s.consumedAt < :now)")
    void deleteExpiredOrConsumed(@Param("now") Instant now);
}

