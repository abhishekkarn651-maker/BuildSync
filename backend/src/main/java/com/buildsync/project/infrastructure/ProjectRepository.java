package com.buildsync.project.infrastructure;

import com.buildsync.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Project entities.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.owner " +
            "WHERE p.id = :id")
    Optional<Project> findByIdWithOwner(@Param("id") UUID id);

    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.owner " +
            "WHERE LOWER(p.slug) = LOWER(:slug)")
    Optional<Project> findBySlugWithOwner(@Param("slug") String slug);

    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.owner " +
            "WHERE p.owner.id = :ownerId " +
            "ORDER BY p.updatedAt DESC")
    List<Project> findByOwnerIdWithOwnerOrderByUpdatedAtDesc(@Param("ownerId") UUID ownerId);

    boolean existsBySlug(String slug);
}

