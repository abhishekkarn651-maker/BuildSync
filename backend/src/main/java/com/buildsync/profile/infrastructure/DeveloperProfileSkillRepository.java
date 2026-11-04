package com.buildsync.profile.infrastructure;

import com.buildsync.profile.domain.DeveloperProfileSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for developer profile assigned skills.
 */
@Repository
public interface DeveloperProfileSkillRepository extends JpaRepository<DeveloperProfileSkill, UUID> {

    @Query("SELECT dps FROM DeveloperProfileSkill dps JOIN FETCH dps.skill WHERE dps.profile.id = :profileId ORDER BY dps.skill.name ASC")
    List<DeveloperProfileSkill> findByProfileIdWithSkill(@Param("profileId") UUID profileId);

    @Query("SELECT dps FROM DeveloperProfileSkill dps JOIN FETCH dps.skill WHERE dps.profile.id IN :profileIds ORDER BY dps.skill.name ASC")
    List<DeveloperProfileSkill> findByProfileIdInWithSkill(@Param("profileIds") Collection<UUID> profileIds);

    @Query("SELECT dps FROM DeveloperProfileSkill dps JOIN FETCH dps.skill WHERE dps.profile.id = :profileId AND dps.skill.id = :skillId")
    Optional<DeveloperProfileSkill> findByProfileIdAndSkillId(@Param("profileId") UUID profileId, @Param("skillId") UUID skillId);

    boolean existsByProfileIdAndSkillId(UUID profileId, UUID skillId);

    @Modifying
    @Query("DELETE FROM DeveloperProfileSkill dps WHERE dps.profile.id = :profileId AND dps.skill.id = :skillId")
    void deleteByProfileIdAndSkillId(@Param("profileId") UUID profileId, @Param("skillId") UUID skillId);
}

