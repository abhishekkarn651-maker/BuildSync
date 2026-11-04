package com.buildsync.profile.api;

import com.buildsync.auth.domain.User;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.ExperienceLevel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Public developer discovery summary payload.
 * Strictly excludes private contact details, email addresses, and credentials.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeveloperSearchResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String fullName;
    private String headline;
    private String bio;
    private String location;
    private String avatarUrl;
    private ExperienceLevel experienceLevel;
    private Integer yearsOfExperience;
    private AvailabilityStatus availabilityStatus;
    private List<ProfileSkillResponse> skills;
    private Instant createdAt;
    private Instant updatedAt;

    public DeveloperSearchResponse() {
        this.skills = new ArrayList<>();
    }

    public DeveloperSearchResponse(UUID id, UUID userId, String username, String fullName,
                                   String headline, String bio, String location, String avatarUrl,
                                   ExperienceLevel experienceLevel, Integer yearsOfExperience,
                                   AvailabilityStatus availabilityStatus, List<ProfileSkillResponse> skills,
                                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.headline = headline;
        this.bio = bio;
        this.location = location;
        this.avatarUrl = avatarUrl;
        this.experienceLevel = experienceLevel;
        this.yearsOfExperience = yearsOfExperience;
        this.availabilityStatus = availabilityStatus;
        this.skills = skills != null ? skills : Collections.emptyList();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static DeveloperSearchResponse fromEntity(DeveloperProfile profile, List<ProfileSkillResponse> skills) {
        User user = profile.getUser();
        return new DeveloperSearchResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getFullName() : null,
                profile.getHeadline(),
                profile.getBio(),
                profile.getLocation(),
                profile.getAvatarUrl(),
                profile.getExperienceLevel(),
                profile.getYearsOfExperience(),
                profile.getAvailabilityStatus(),
                skills != null ? skills : Collections.emptyList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public ExperienceLevel getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(ExperienceLevel experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(Integer yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public List<ProfileSkillResponse> getSkills() {
        return skills;
    }

    public void setSkills(List<ProfileSkillResponse> skills) {
        this.skills = skills != null ? skills : Collections.emptyList();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

