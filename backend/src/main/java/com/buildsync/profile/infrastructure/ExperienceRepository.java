package com.buildsync.profile.infrastructure;

import com.buildsync.profile.domain.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for developer profile work experience records.
 */
@Repository
public interface ExperienceRepository extends JpaRepository<WorkExperience, UUID> {

    List<WorkExperience> findByProfileIdOrderByStartDateDesc(UUID profileId);

    Optional<WorkExperience> findByIdAndProfileId(UUID id, UUID profileId);

    void deleteByIdAndProfileId(UUID id, UUID profileId);
}

