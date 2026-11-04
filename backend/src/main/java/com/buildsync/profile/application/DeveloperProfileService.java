package com.buildsync.profile.application;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.DuplicateResourceException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.common.response.PagedResponse;
import com.buildsync.media.application.ImageValidator;
import com.buildsync.media.application.StorageService;
import com.buildsync.profile.api.CreateProfileRequest;
import com.buildsync.profile.api.DeveloperSearchResponse;
import com.buildsync.profile.api.EducationResponse;
import com.buildsync.profile.api.ExperienceResponse;
import com.buildsync.profile.api.ProfileResponse;
import com.buildsync.profile.api.ProfileSkillResponse;
import com.buildsync.profile.api.UpdateProfileRequest;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.DeveloperProfileSkill;
import com.buildsync.profile.domain.ExperienceLevel;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSpecifications;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing developer profile lifecycle, ownership constraints, and queries.
 */
@Service
@Transactional(readOnly = true)
public class DeveloperProfileService {

    private static final Logger log = LoggerFactory.getLogger(DeveloperProfileService.class);

    private final DeveloperProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final DeveloperProfileSkillRepository profileSkillRepository;
    private final EducationRepository educationRepository;
    private final ExperienceRepository experienceRepository;
    private final StorageService storageService;
    private final ImageValidator imageValidator;

    public DeveloperProfileService(
            DeveloperProfileRepository profileRepository,
            UserRepository userRepository,
            DeveloperProfileSkillRepository profileSkillRepository,
            EducationRepository educationRepository,
            ExperienceRepository experienceRepository,
            StorageService storageService,
            ImageValidator imageValidator) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.profileSkillRepository = profileSkillRepository;
        this.educationRepository = educationRepository;
        this.experienceRepository = experienceRepository;
        this.storageService = storageService;
        this.imageValidator = imageValidator;
    }

    /**
     * Create a new profile for the authenticated user.
     * Prevents duplicate profile creation for the same user account.
     */
    @Transactional
    public ProfileResponse createProfile(UUID userId, CreateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (profileRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("Developer profile already exists for user: " + user.getUsername());
        }

        DeveloperProfile profile = new DeveloperProfile(user);
        profile.setHeadline(request.getHeadline());
        profile.setBio(request.getBio());
        profile.setAbout(request.getAbout());
        profile.setLocation(request.getLocation());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setTwitterUrl(request.getTwitterUrl());
        profile.setExperienceLevel(request.getExperienceLevel());
        profile.setYearsOfExperience(request.getYearsOfExperience());
        profile.setAvailabilityStatus(request.getAvailabilityStatus() != null
                ? request.getAvailabilityStatus()
                : AvailabilityStatus.AVAILABLE);

        DeveloperProfile savedProfile = profileRepository.save(profile);
        log.info("Created developer profile for user '{}' with profile id '{}'", user.getUsername(), savedProfile.getId());

        return ProfileResponse.fromEntity(savedProfile);
    }

    /**
     * Retrieve profile of the currently authenticated user including skills, education, and experience.
     */
    public ProfileResponse getProfileByUserId(UUID userId) {
        DeveloperProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user id: " + userId));

        return buildDetailedProfileResponse(profile);
    }

    /**
     * Update the profile of the currently authenticated user.
     * Guarantees strict profile ownership enforcement.
     */
    @Transactional
    public ProfileResponse updateProfileByUserId(UUID userId, UpdateProfileRequest request) {
        DeveloperProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user id: " + userId));

        profile.setHeadline(request.getHeadline());
        profile.setBio(request.getBio());
        profile.setAbout(request.getAbout());
        profile.setLocation(request.getLocation());
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setGithubUrl(request.getGithubUrl());
        profile.setLinkedinUrl(request.getLinkedinUrl());
        profile.setTwitterUrl(request.getTwitterUrl());
        profile.setExperienceLevel(request.getExperienceLevel());
        profile.setYearsOfExperience(request.getYearsOfExperience());

        if (request.getAvailabilityStatus() != null) {
            profile.setAvailabilityStatus(request.getAvailabilityStatus());
        }

        DeveloperProfile updatedProfile = profileRepository.save(profile);
        log.info("Updated developer profile id '{}' for user '{}'", updatedProfile.getId(), profile.getUser().getUsername());

        return buildDetailedProfileResponse(updatedProfile);
    }

    /**
     * Upload and update the avatar for the authenticated user's profile.
     * Validates image content, stores the file, updates the database,
     * and deletes the old avatar safely after database persistence succeeds.
     */
    @Transactional
    public ProfileResponse uploadAvatar(UUID userId, org.springframework.web.multipart.MultipartFile file) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        imageValidator.validate(file);

        DeveloperProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user ID: " + userId));

        String oldAvatarUrl = profile.getAvatarUrl();
        String newAvatarUrl = storageService.storeAvatar(file);

        profile.setAvatarUrl(newAvatarUrl);
        DeveloperProfile savedProfile = profileRepository.save(profile);
        log.info("Updated avatar for developer profile '{}' to '{}'", profile.getId(), newAvatarUrl);

        // Safe deletion of old avatar only AFTER database update succeeds
        if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
            try {
                storageService.deleteAvatar(oldAvatarUrl);
            } catch (Exception e) {
                log.warn("Failed to delete previous avatar file: {}", oldAvatarUrl, e);
            }
        }

        return buildDetailedProfileResponse(savedProfile);
    }

    /**
     * Remove the avatar from the authenticated user's profile.
     * Clears the database reference and deletes the file from disk.
     */
    @Transactional
    public ProfileResponse deleteAvatar(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        DeveloperProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user ID: " + userId));

        String oldAvatarUrl = profile.getAvatarUrl();
        if (oldAvatarUrl == null || oldAvatarUrl.isBlank()) {
            return buildDetailedProfileResponse(profile);
        }

        profile.setAvatarUrl(null);
        DeveloperProfile savedProfile = profileRepository.save(profile);
        log.info("Removed avatar for developer profile '{}'", profile.getId());

        try {
            storageService.deleteAvatar(oldAvatarUrl);
        } catch (Exception e) {
            log.warn("Failed to delete avatar file on avatar removal: {}", oldAvatarUrl, e);
        }

        return buildDetailedProfileResponse(savedProfile);
    }

    /**
     * Retrieve a developer's public profile by their unique username,
     * including technical skills, education history, and professional experience.
     */
    public ProfileResponse getProfileByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        String normalizedUsername = username.trim().toLowerCase();

        DeveloperProfile profile = profileRepository.findByUsernameWithUser(normalizedUsername)
                .orElseGet(() -> {
                    // Check whether user exists without a profile, or user doesn't exist at all
                    boolean userExists = userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent();
                    if (!userExists) {
                        throw new ResourceNotFoundException("User not found with username: " + username);
                    }
                    throw new ResourceNotFoundException("Developer profile not found for username: " + username);
                });

        return buildDetailedProfileResponse(profile);
    }

    /**
     * Search public developer profiles with multi-criteria filtering, pagination, and sorting.
     */
    public PagedResponse<DeveloperSearchResponse> searchDevelopers(
            String keyword,
            String skill,
            ExperienceLevel experienceLevel,
            AvailabilityStatus availability,
            String location,
            int page,
            int size,
            String sort) {

        if (page < 0) {
            throw new IllegalArgumentException("Page index must not be less than zero");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must not be less than one");
        }
        if (size > 50) {
            throw new IllegalArgumentException("Page size must not exceed 50");
        }

        Sort sortObj = resolveSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Specification<DeveloperProfile> spec = DeveloperProfileSpecifications.withFilters(
                keyword, skill, experienceLevel, availability, location);

        Page<DeveloperProfile> profilePage = profileRepository.findAll(spec, pageable);

        if (profilePage.isEmpty()) {
            return new PagedResponse<>(
                    Collections.emptyList(),
                    profilePage.getNumber(),
                    profilePage.getSize(),
                    profilePage.getTotalElements(),
                    profilePage.getTotalPages(),
                    profilePage.isLast()
            );
        }

        List<UUID> profileIds = profilePage.getContent().stream()
                .map(DeveloperProfile::getId)
                .collect(Collectors.toList());

        List<DeveloperProfileSkill> allSkills = profileSkillRepository.findByProfileIdInWithSkill(profileIds);

        Map<UUID, List<ProfileSkillResponse>> skillsByProfileId = allSkills.stream()
                .collect(Collectors.groupingBy(
                        dps -> dps.getProfile().getId(),
                        Collectors.mapping(ProfileSkillResponse::fromEntity, Collectors.toList())
                ));

        List<DeveloperSearchResponse> responses = profilePage.getContent().stream()
                .map(p -> DeveloperSearchResponse.fromEntity(
                        p,
                        skillsByProfileId.getOrDefault(p.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());

        return new PagedResponse<>(
                responses,
                profilePage.getNumber(),
                profilePage.getSize(),
                profilePage.getTotalElements(),
                profilePage.getTotalPages(),
                profilePage.isLast()
        );
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.trim().isEmpty() || sort.equalsIgnoreCase("recently_updated")) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }
        if (sort.equalsIgnoreCase("newest")) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        if (sort.equalsIgnoreCase("name_asc")) {
            return Sort.by(Sort.Direction.ASC, "user.fullName");
        }
        if (sort.equalsIgnoreCase("name_desc")) {
            return Sort.by(Sort.Direction.DESC, "user.fullName");
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        if (field.equals("updatedAt") || field.equals("createdAt") || field.equals("yearsOfExperience")) {
            return Sort.by(direction, field);
        }
        if (field.equals("fullName") || field.equals("name") || field.equals("user.fullName")) {
            return Sort.by(direction, "user.fullName");
        }

        return Sort.by(Sort.Direction.DESC, "updatedAt");
    }

    private ProfileResponse buildDetailedProfileResponse(DeveloperProfile profile) {
        List<ProfileSkillResponse> skills = profileSkillRepository.findByProfileIdWithSkill(profile.getId())
                .stream()
                .map(ProfileSkillResponse::fromEntity)
                .collect(Collectors.toList());

        List<EducationResponse> education = educationRepository.findByProfileIdOrderByStartDateDesc(profile.getId())
                .stream()
                .map(EducationResponse::fromEntity)
                .collect(Collectors.toList());

        List<ExperienceResponse> experience = experienceRepository.findByProfileIdOrderByStartDateDesc(profile.getId())
                .stream()
                .map(ExperienceResponse::fromEntity)
                .collect(Collectors.toList());

        return ProfileResponse.fromEntityWithDetails(profile, skills, education, experience);
    }
}
