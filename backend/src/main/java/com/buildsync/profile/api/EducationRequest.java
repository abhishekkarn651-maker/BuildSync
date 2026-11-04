package com.buildsync.profile.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request payload for creating or updating an education record.
 */
public class EducationRequest {

    @NotBlank(message = "Institution name is required")
    @Size(max = 200, message = "Institution must not exceed 200 characters")
    private String institution;

    @Size(max = 150, message = "Degree must not exceed 150 characters")
    private String degree;

    @Size(max = 150, message = "Field of study must not exceed 150 characters")
    private String fieldOfStudy;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean currentlyStudying;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    public EducationRequest() {
    }

    public EducationRequest(String institution, String degree, String fieldOfStudy,
                            LocalDate startDate, LocalDate endDate, Boolean currentlyStudying, String description) {
        this.institution = institution;
        this.degree = degree;
        this.fieldOfStudy = fieldOfStudy;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentlyStudying = currentlyStudying;
        this.description = description;
    }

    @AssertTrue(message = "End date must not precede start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "End date must be null when currently studying")
    public boolean isCurrentlyStudyingValid() {
        if (Boolean.TRUE.equals(currentlyStudying)) {
            return endDate == null;
        }
        return true;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
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

    public Boolean getCurrentlyStudying() {
        return currentlyStudying;
    }

    public void setCurrentlyStudying(Boolean currentlyStudying) {
        this.currentlyStudying = currentlyStudying;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

