package com.buildsync.profile.infrastructure;

import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for master skills catalogue.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    List<Skill> findAllByOrderByNameAsc();

    List<Skill> findByCategoryOrderByNameAsc(SkillCategory category);

    List<Skill> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Skill> findByCategoryAndNameContainingIgnoreCaseOrderByNameAsc(SkillCategory category, String name);
}
