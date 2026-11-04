package com.buildsync.profile;

import com.buildsync.auth.domain.User;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.EducationRequest;
import com.buildsync.profile.api.EducationResponse;
import com.buildsync.profile.application.EducationService;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.Education;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EducationServiceTest {

    @Mock
    private EducationRepository educationRepository;

    @Mock
    private DeveloperProfileRepository profileRepository;

    @InjectMocks
    private EducationService educationService;

    private UUID userId;
    private DeveloperProfile testProfile;
    private UUID educationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User("Jane Dev", "janedev", "jane@buildsync.dev", "password");
        user.setId(userId);

        testProfile = new DeveloperProfile(user);
        testProfile.setId(UUID.randomUUID());

        educationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getEducation returns education records for user profile")
    void getEducation_Success() {
        Education edu = new Education(testProfile, "Stanford University", "B.S.", "Computer Science",
                LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 1), false, "Focused on algorithms");
        edu.setId(educationId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findByProfileIdOrderByStartDateDesc(testProfile.getId()))
                .thenReturn(Collections.singletonList(edu));

        List<EducationResponse> result = educationService.getEducation(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInstitution()).isEqualTo("Stanford University");
    }

    @Test
    @DisplayName("addEducation persists valid education entry")
    void addEducation_Success() {
        EducationRequest request = new EducationRequest(
                "Stanford University", "B.S.", "Computer Science",
                LocalDate.of(2020, 9, 1), LocalDate.of(2024, 6, 1), false, "Algorithms"
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        Education saved = new Education(testProfile, request.getInstitution(), request.getDegree(),
                request.getFieldOfStudy(), request.getStartDate(), request.getEndDate(), false, request.getDescription());
        saved.setId(educationId);

        when(educationRepository.save(any(Education.class))).thenReturn(saved);

        EducationResponse response = educationService.addEducation(userId, request);

        assertThat(response.getId()).isEqualTo(educationId);
        assertThat(response.getInstitution()).isEqualTo("Stanford University");
    }

    @Test
    @DisplayName("addEducation throws exception when end date precedes start date")
    void addEducation_InvalidDateRangeThrows() {
        EducationRequest request = new EducationRequest(
                "Stanford University", "B.S.", "Computer Science",
                LocalDate.of(2024, 9, 1), LocalDate.of(2020, 6, 1), false, "Invalid"
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        assertThatThrownBy(() -> educationService.addEducation(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot precede start date");
    }

    @Test
    @DisplayName("addEducation throws exception when currentlyStudying is true and endDate is provided")
    void addEducation_CurrentlyStudyingWithEndDateThrows() {
        EducationRequest request = new EducationRequest(
                "Stanford University", "B.S.", "Computer Science",
                LocalDate.of(2022, 9, 1), LocalDate.of(2026, 6, 1), true, "Invalid"
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        assertThatThrownBy(() -> educationService.addEducation(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be null when currently studying");
    }

    @Test
    @DisplayName("deleteEducation deletes record successfully")
    void deleteEducation_Success() {
        Education edu = new Education(testProfile, "MIT", "M.S.", "AI",
                LocalDate.of(2022, 9, 1), null, true, "AI research");
        edu.setId(educationId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findByIdAndProfileId(educationId, testProfile.getId())).thenReturn(Optional.of(edu));

        educationService.deleteEducation(userId, educationId);

        verify(educationRepository).delete(edu);
    }

    @Test
    @DisplayName("deleteEducation throws ResourceNotFoundException if record not found")
    void deleteEducation_NotFoundThrows() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(educationRepository.findByIdAndProfileId(educationId, testProfile.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> educationService.deleteEducation(userId, educationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Education record not found");
    }
}

