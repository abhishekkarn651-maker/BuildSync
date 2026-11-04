package com.buildsync.profile.api;

import com.buildsync.profile.domain.EmploymentType;
import com.buildsync.profile.domain.WorkExperience;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Safe DTO representing a professional work experience entry.
 */
public class ExperienceResponse {

    private UUID id;
    private String jobTitle;
    private String companyName;
    private EmploymentType employmentType;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean currentlyWorking;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public ExperienceResponse() {
    }

    public ExperienceResponse(UUID id, String jobTitle, String companyName, EmploymentType employmentType,
                              String location, LocalDate startDate, LocalDate endDate,
                              Boolean currentlyWorking, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.employmentType = employmentType;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyWorking = currentlyWorking;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ExperienceResponse fromEntity(WorkExperience experience) {
        return new ExperienceResponse(
                experience.getId(),
                experience.getJobTitle(),
                experience.getCompanyName(),
                experience.getEmploymentType(),
                experience.getLocation(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.getCurrentlyWorking(),
                experience.getDescription(),
                experience.getCreatedAt(),
                experience.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getCurrentlyWorking() {
        return currentlyWorking;
    }

    public void setCurrentlyWorking(Boolean currentlyWorking) {
        this.currentlyWorking = currentlyWorking;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

