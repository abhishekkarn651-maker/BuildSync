package com.buildsync.profile.application;

import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.EducationRequest;
import com.buildsync.profile.api.EducationResponse;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.Education;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing education history for developer profiles with strict ownership enforcement.
 */
@Service
@Transactional(readOnly = true)
public class EducationService {

    private static final Logger log = LoggerFactory.getLogger(EducationService.class);

    private final EducationRepository educationRepository;
    private final DeveloperProfileRepository profileRepository;

    public EducationService(EducationRepository educationRepository, DeveloperProfileRepository profileRepository) {
        this.educationRepository = educationRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * Retrieve all education records for the authenticated user's profile.
     */
    public List<EducationResponse> getEducation(UUID userId) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        return educationRepository.findByProfileIdOrderByStartDateDesc(profile.getId())
                .stream()
                .map(EducationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Add a new education entry to the authenticated user's profile.
     */
    @Transactional
    public EducationResponse addEducation(UUID userId, EducationRequest request) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        validateDates(request);

        Education education = new Education(
                profile,
                request.getInstitution(),
                request.getDegree(),
                request.getFieldOfStudy(),
                request.getStartDate(),
                request.getEndDate(),
                request.getCurrentlyStudying(),
                request.getDescription()
        );

        Education saved = educationRepository.save(education);
        log.info("Added education entry id '{}' at '{}' for profile id '{}'",
                saved.getId(), saved.getInstitution(), profile.getId());

        return EducationResponse.fromEntity(saved);
    }

    /**
     * Update an existing education entry owned by the authenticated user.
     */
    @Transactional
    public EducationResponse updateEducation(UUID userId, UUID educationId, EducationRequest request) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        validateDates(request);

        Education education = educationRepository.findByIdAndProfileId(educationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education record not found with id: " + educationId));

        education.setInstitution(request.getInstitution());
        education.setDegree(request.getDegree());
        education.setFieldOfStudy(request.getFieldOfStudy());
        education.setStartDate(request.getStartDate());
        education.setCurrentlyStudying(request.getCurrentlyStudying());
        education.setEndDate(request.getEndDate());
        education.setDescription(request.getDescription());

        Education updated = educationRepository.save(education);
        log.info("Updated education entry id '{}' for profile id '{}'", updated.getId(), profile.getId());

        return EducationResponse.fromEntity(updated);
    }

    /**
     * Delete an education entry owned by the authenticated user.
     */
    @Transactional
    public void deleteEducation(UUID userId, UUID educationId) {
        DeveloperProfile profile = getProfileOrThrow(userId);

        Education education = educationRepository.findByIdAndProfileId(educationId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Education record not found with id: " + educationId));

        educationRepository.delete(education);
        log.info("Deleted education entry id '{}' from profile id '{}'", educationId, profile.getId());
    }

    private DeveloperProfile getProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user. Please create your profile first."));
    }

    private void validateDates(EducationRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new IllegalArgumentException("End date cannot precede start date");
            }
        }
        if (Boolean.TRUE.equals(request.getCurrentlyStudying()) && request.getEndDate() != null) {
            throw new IllegalArgumentException("End date must be null when currently studying");
        }
    }
}

