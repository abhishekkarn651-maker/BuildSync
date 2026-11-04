package com.buildsync.profile.application;

import com.buildsync.common.exception.DuplicateResourceException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.AddSkillRequest;
import com.buildsync.profile.api.ProfileSkillResponse;
import com.buildsync.profile.api.SkillResponse;
import com.buildsync.profile.api.UpdateSkillProficiencyRequest;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.DeveloperProfileSkill;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing master skill catalogue queries and developer profile skill assignments.
 */
@Service
@Transactional(readOnly = true)
public class SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    private final SkillRepository skillRepository;
    private final DeveloperProfileRepository profileRepository;
    private final DeveloperProfileSkillRepository profileSkillRepository;

    public SkillService(
            SkillRepository skillRepository,
            DeveloperProfileRepository profileRepository,
            DeveloperProfileSkillRepository profileSkillRepository) {
        this.skillRepository = skillRepository;
        this.profileRepository = profileRepository;
        this.profileSkillRepository = profileSkillRepository;
    }

    /**
     * Search skills in master catalogue with optional text query and category filter.
     */
    public List<SkillResponse> searchSkills(String query, SkillCategory category) {
        String trimmedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;

        List<Skill> skills;
        if (trimmedQuery != null && category != null) {
            skills = skillRepository.findByCategoryAndNameContainingIgnoreCaseOrderByNameAsc(category, trimmedQuery);
        } else if (trimmedQuery != null) {
            skills = skillRepository.findByNameContainingIgnoreCaseOrderByNameAsc(trimmedQuery);
        } else if (category != null) {
            skills = skillRepository.findByCategoryOrderByNameAsc(category);
        } else {
            skills = skillRepository.findAllByOrderByNameAsc();
        }

        return skills.stream()
                .map(SkillResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all skills assigned to the authenticated user's profile.
     */
    public List<ProfileSkillResponse> getProfileSkills(UUID userId) {
        DeveloperProfile profile = getProfileOrThrow(userId);
        return profileSkillRepository.findByProfileIdWithSkill(profile.getId())
                .stream()
                .map(ProfileSkillResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Add a skill to the authenticated user's profile.
     * Prevents duplicate skill assignments.
     */
    @Transactional
    public ProfileSkillResponse addSkillToProfile(UUID userId, AddSkillRequest request) {
        DeveloperProfile profile = getProfileOrThrow(userId);

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id: " + request.getSkillId()));

        if (profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), skill.getId())) {
            throw new DuplicateResourceException("Skill already assigned to developer profile: " + skill.getName());
        }

        DeveloperProfileSkill profileSkill = new DeveloperProfileSkill(
                profile,
                skill,
                request.getProficiencyLevel()
        );

        DeveloperProfileSkill saved = profileSkillRepository.save(profileSkill);
        log.info("Assigned skill '{}' ({}) to profile id '{}'", skill.getName(), request.getProficiencyLevel(), profile.getId());

        return ProfileSkillResponse.fromEntity(saved);
    }

    /**
     * Update proficiency level for an assigned skill on authenticated user's profile.
     */
    @Transactional
    public ProfileSkillResponse updateSkillProficiency(UUID userId, UUID skillId, UpdateSkillProficiencyRequest request) {
        DeveloperProfile profile = getProfileOrThrow(userId);

        DeveloperProfileSkill profileSkill = profileSkillRepository.findByProfileIdAndSkillId(profile.getId(), skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill with id '" + skillId + "' is not assigned to this profile"));

        profileSkill.setProficiencyLevel(request.getProficiencyLevel());
        DeveloperProfileSkill updated = profileSkillRepository.save(profileSkill);
        log.info("Updated proficiency to '{}' for skill id '{}' on profile id '{}'",
                request.getProficiencyLevel(), skillId, profile.getId());

        return ProfileSkillResponse.fromEntity(updated);
    }

    /**
     * Remove an assigned skill from the authenticated user's profile.
     * Does NOT remove the skill from the master catalogue.
     */
    @Transactional
    public void removeSkillFromProfile(UUID userId, UUID skillId) {
        DeveloperProfile profile = getProfileOrThrow(userId);

        if (!profileSkillRepository.existsByProfileIdAndSkillId(profile.getId(), skillId)) {
            throw new ResourceNotFoundException("Skill with id '" + skillId + "' is not assigned to this profile");
        }

        profileSkillRepository.deleteByProfileIdAndSkillId(profile.getId(), skillId);
        log.info("Removed skill id '{}' from profile id '{}'", skillId, profile.getId());
    }

    private DeveloperProfile getProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Developer profile not found for user. Please create your profile first."));
    }
}
