package com.buildsync.collaboration.infrastructure;

import com.buildsync.collaboration.domain.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ProjectMember entities.
 */
@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    long countByProjectId(UUID projectId);

    @Query("SELECT pm FROM ProjectMember pm " +
            "JOIN FETCH pm.user u " +
            "WHERE pm.project.id = :projectId " +
            "ORDER BY pm.joinedAt ASC")
    List<ProjectMember> findByProjectIdWithUser(@Param("projectId") UUID projectId);

    @Query("SELECT pm FROM ProjectMember pm " +
            "JOIN FETCH pm.project p " +
            "JOIN FETCH p.owner o " +
            "WHERE pm.user.id = :userId " +
            "ORDER BY pm.joinedAt DESC")
    List<ProjectMember> findByUserIdWithProject(@Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    void deleteByProjectIdAndUserId(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProjectMember pm WHERE pm.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}

