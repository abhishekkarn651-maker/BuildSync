package com.buildsync.project.infrastructure;

import com.buildsync.project.domain.ProjectTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for ProjectTechnology join entities.
 */
@Repository
public interface ProjectTechnologyRepository extends JpaRepository<ProjectTechnology, UUID> {

    @Query("SELECT pt FROM ProjectTechnology pt " +
            "JOIN FETCH pt.skill " +
            "WHERE pt.project.id = :projectId " +
            "ORDER BY pt.skill.name ASC")
    List<ProjectTechnology> findByProjectIdWithSkill(@Param("projectId") UUID projectId);

    @Query("SELECT pt FROM ProjectTechnology pt " +
            "JOIN FETCH pt.skill " +
            "WHERE pt.project.id IN :projectIds " +
            "ORDER BY pt.skill.name ASC")
    List<ProjectTechnology> findByProjectIdInWithSkill(@Param("projectIds") Collection<UUID> projectIds);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("DELETE FROM ProjectTechnology pt WHERE pt.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") UUID projectId);
}
