package com.buildsync.profile.application;

import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.ExperienceRequest;
import com.buildsync.profile.api.ExperienceResponse;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.WorkExperience;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing professional work experience for developer profiles with strict ownership enforcement.
 */
@Service
@Transactional(readOnly = true)
public class ExperienceService {

    private static final Logger log = LoggerFactory.getLogger(ExperienceService.class);

    private final ExperienceRepository experienceRepository;
    private final DeveloperProfileRepository profileRepository;

    public ExperienceService(ExperienceRepository experienceRepository, DeveloperProfileRepository profileRepository) {
        this.experienceRepository = experienceRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * Retrieve all work experience entries for the authenticated user's profile.
     */
    public List<ExperienceResponse> getExperience(UUID userId) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        return experienceRepository.findByProfileIdOrderByStartDateDesc(profile.getId())
                .stream()
                .map(ExperienceResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Add a new work experience entry to the authenticated user's profile.
     */
    @Transactional
    public ExperienceResponse addExperience(UUID userId, ExperienceRequest request) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        validateDates(request);

        WorkExperience experience = new WorkExperience(
                profile,
                request.getJobTitle(),
                request.getCompanyName(),
                request.getEmploymentType(),
                request.getLocation(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCurrentlyWorking(),
                request.getDescription()
        );

        WorkExperience saved = experienceRepository.save(experience);
        log.info("Added work experience entry id '{}' at '{}' for profile id '{}'",
                saved.getId(), saved.getCompanyName(), profile.getId());

        return ExperienceResponse.fromEntity(saved);
    }

    /**
     * Update an existing work experience entry owned by the authenticated user.
     */
    @Transactional
    public ExperienceResponse updateExperience(UUID userId, UUID experienceId, ExperienceRequest request) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        validateDates(request);

        WorkExperience experience = experienceRepository.findByIdAndProfileId(experienceId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Work experience record not found with id: " + experienceId));

        experience.setJobTitle(request.getJobTitle());
        experience.setCompanyName(request.getCompanyName());
        experience.setEmploymentType(request.getEmploymentType());
        experience.setLocation(request.getLocation());
        experience.setStartDate(request.getStartDate());
        experience.setCurrentlyWorking(request.getCurrentlyWorking());
        experience.setEndDate(request.getEndDate());
        experience.setDescription(request.getDescription());

        WorkExperience updated = experienceRepository.save(experience);
        log.info("Updated work experience entry id '{}' for profile id '{}'", updated.getId(), profile.getId());

        return ExperienceResponse.fromEntity(updated);
    }

    /**
     * Delete a work experience entry owned by the authenticated user.
     */
    @Transactional
    public void deleteExperience(UUID userId, UUID experienceId) {
        DeveloperProfile profile = getProfileOrThrow(userId);

        WorkExperience experience = experienceRepository.findByIdAndProfileId(experienceId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Work experience record not found with id: " + experienceId));

        experienceRepository.delete(experience);
        log.info("Deleted work experience entry id '{}' from profile id '{}'", experienceId, profile.getId());
    }

    private DeveloperProfile getProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user. Please create your profile first."));
    }

    private void validateDates(ExperienceRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date cannot precede start date");
            }
        }
        if (Boolean.TRUE.equals(request.getCurrentlyWorking()) && request.getEndDate() != null) {
            throw new IllegalArgumentException("End date must be null when currently working");
        }
    }
}

