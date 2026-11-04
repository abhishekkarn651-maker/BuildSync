package com.buildsync.profile.infrastructure;

import com.buildsync.profile.domain.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for developer profile education records.
 */
@Repository
public interface EducationRepository extends JpaRepository<Education, UUID> {

    List<Education> findByProfileIdOrderByStartDateDesc(UUID profileId);

    Optional<Education> findByIdAndProfileId(UUID id, UUID profileId);

    void deleteByIdAndProfileId(UUID id, UUID profileId);
}

