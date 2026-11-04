package com.buildsync.profile.api;

import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.ExperienceLevel;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new developer profile.
 */
public class CreateProfileRequest {

    @Size(max = 150, message = "Headline cannot exceed 150 characters")
    private String headline;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    private String about;

    @Size(max = 100, message = "Location cannot exceed 100 characters")
    private String location;

    @Size(max = 255, message = "Website URL cannot exceed 255 characters")
    @Pattern(regexp = "^(https?://.+)?$", message = "Website URL must start with http:// or https://")
    private String websiteUrl;

    @Size(max = 255, message = "GitHub URL cannot exceed 255 characters")
    @Pattern(regexp = "^(https?://.+)?$", message = "GitHub URL must start with http:// or https://")
    private String githubUrl;

    @Size(max = 255, message = "LinkedIn URL cannot exceed 255 characters")
    @Pattern(regexp = "^(https?://.+)?$", message = "LinkedIn URL must start with http:// or https://")
    private String linkedinUrl;

    @Size(max = 255, message = "Twitter URL cannot exceed 255 characters")
    @Pattern(regexp = "^(https?://.+)?$", message = "Twitter URL must start with http:// or https://")
    private String twitterUrl;

    private ExperienceLevel experienceLevel;

    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 100, message = "Years of experience cannot exceed 100")
    private Integer yearsOfExperience;

    private AvailabilityStatus availabilityStatus;

    public CreateProfileRequest() {
    }

    public CreateProfileRequest(String headline, String bio, String about, String location,
                                String websiteUrl, String githubUrl, String linkedinUrl, String twitterUrl,
                                ExperienceLevel experienceLevel, Integer yearsOfExperience, AvailabilityStatus availabilityStatus) {
        this.headline = headline;
        this.bio = bio;
        this.about = about;
        this.location = location;
        this.websiteUrl = websiteUrl;
        this.githubUrl = githubUrl;
        this.linkedinUrl = linkedinUrl;
        this.twitterUrl = twitterUrl;
        this.experienceLevel = experienceLevel;
        this.yearsOfExperience = yearsOfExperience;
        this.availabilityStatus = availabilityStatus;
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
}

