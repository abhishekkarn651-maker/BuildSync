package com.buildsync.profile.api;

import com.buildsync.auth.domain.User;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.ExperienceLevel;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Safe profile response payload representing developer details, identity,
 * skills, education history, and professional experience.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfileResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private String fullName;
    private String email;
    private String headline;
    private String bio;
    private String about;
    private String location;
    private String websiteUrl;
    private String githubUrl;
    private String linkedinUrl;
    private String twitterUrl;
    private String avatarUrl;
    private ExperienceLevel experienceLevel;
    private Integer yearsOfExperience;
    private AvailabilityStatus availabilityStatus;
    private List<ProfileSkillResponse> skills;
    private List<EducationResponse> education;
    private List<ExperienceResponse> experience;
    private Instant createdAt;
    private Instant updatedAt;

    public ProfileResponse() {
    }

    public ProfileResponse(UUID id, UUID userId, String username, String fullName, String email,
                           String headline, String bio, String about, String location,
                           String websiteUrl, String githubUrl, String linkedinUrl, String twitterUrl,
                           String avatarUrl, ExperienceLevel experienceLevel, Integer yearsOfExperience,
                           AvailabilityStatus availabilityStatus, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.headline = headline;
        this.bio = bio;
        this.about = about;
        this.location = location;
        this.websiteUrl = websiteUrl;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.twitterUrl = twitterUrl;
        this.avatarUrl = avatarUrl;
        this.experienceLevel = experienceLevel;
        this.yearsOfExperience = yearsOfExperience;
        this.availabilityStatus = availabilityStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProfileResponse fromEntity(DeveloperProfile profile) {
        User user = profile.getUser();
        return new ProfileResponse(
                profile.getId(),
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getFullName() : null,
                user != null ? user.getEmail() : null,
                profile.getHeadline(),
                profile.getBio(),
                profile.getAbout(),
                profile.getLocation(),
                profile.getWebsiteUrl(),
                profile.getGithubUrl(),
                profile.getLinkedinUrl(),
                profile.getTwitterUrl(),
                profile.getAvatarUrl(),
                profile.getExperienceLevel(),
                profile.getYearsOfExperience(),
                profile.getAvailabilityStatus(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    public static ProfileResponse fromEntityWithDetails(
            DeveloperProfile profile,
            List<ProfileSkillResponse> skills,
            List<EducationResponse> education,
            List<ExperienceResponse> experience) {
        ProfileResponse response = fromEntity(profile);
        response.setSkills(skills != null ? skills : new ArrayList<>());
        response.setEducation(education != null ? education : new ArrayList<>());
        response.setExperience(experience != null ? experience : new ArrayList<>());
        return response;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getLinkedinUrl() {
        return linkedinUrl;
    }

    public void setLinkedinUrl(String linkedinUrl) {
        this.linkedinUrl = linkedinUrl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public void setTwitterUrl(String twitterUrl) {
        this.twitterUrl = twitterUrl;
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
        this.skills = skills;
    }

    public List<EducationResponse> getEducation() {
        return education;
    }

    public void setEducation(List<EducationResponse> education) {
        this.education = education;
    }

    public List<ExperienceResponse> getExperience() {
        return experience;
    }

    public void setExperience(List<ExperienceResponse> experience) {
        this.experience = experience;
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
