package com.buildsync.profile;

import com.buildsync.auth.domain.User;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.ExperienceRequest;
import com.buildsync.profile.api.ExperienceResponse;
import com.buildsync.profile.application.ExperienceService;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.EmploymentType;
import com.buildsync.profile.domain.WorkExperience;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
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
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private DeveloperProfileRepository profileRepository;

    @InjectMocks
    private ExperienceService experienceService;

    private UUID userId;
    private DeveloperProfile testProfile;
    private UUID experienceId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User("Jane Dev", "janedev", "jane@buildsync.dev", "password");
        user.setId(userId);

        testProfile = new DeveloperProfile(user);
        testProfile.setId(UUID.randomUUID());

        experienceId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getExperience returns work experience records for user profile")
    void getExperience_Success() {
        WorkExperience exp = new WorkExperience(testProfile, "Staff Engineer", "Tech Corp",
                EmploymentType.FULL_TIME, "San Francisco, CA", LocalDate.of(2022, 1, 1),
                null, true, "Led backend architecture");
        exp.setId(experienceId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(experienceRepository.findByProfileIdOrderByStartDateDesc(testProfile.getId()))
                .thenReturn(Collections.singletonList(exp));

        List<ExperienceResponse> result = experienceService.getExperience(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getJobTitle()).isEqualTo("Staff Engineer");
        assertThat(result.get(0).getCompanyName()).isEqualTo("Tech Corp");
    }

    @Test
    @DisplayName("addExperience persists valid experience entry")
    void addExperience_Success() {
        ExperienceRequest request = new ExperienceRequest(
                "Senior Developer", "Cloud Inc", EmploymentType.FULL_TIME,
                "Remote", LocalDate.of(2021, 6, 1), LocalDate.of(2023, 12, 31),
                false, "Cloud native microservices"
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        WorkExperience saved = new WorkExperience(testProfile, request.getJobTitle(), request.getCompanyName(),
                request.getEmploymentType(), request.getLocation(), request.getStartDate(),
                request.getEndDate(), false, request.getDescription());
        saved.setId(experienceId);

        when(experienceRepository.save(any(WorkExperience.class))).thenReturn(saved);

        ExperienceResponse response = experienceService.addExperience(userId, request);

        assertThat(response.getId()).isEqualTo(experienceId);
        assertThat(response.getJobTitle()).isEqualTo("Senior Developer");
    }

    @Test
    @DisplayName("addExperience throws exception when end date precedes start date")
    void addExperience_InvalidDateRangeThrows() {
        ExperienceRequest request = new ExperienceRequest(
                "Developer", "Acme Corp", EmploymentType.FULL_TIME,
                "Remote", LocalDate.of(2024, 1, 1), LocalDate.of(2022, 1, 1),
                false, "Invalid dates"
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        assertThatThrownBy(() -> experienceService.addExperience(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date cannot precede start date");
    }

    @Test
    @DisplayName("addExperience throws exception when currentlyWorking is true and endDate is provided")
    void addExperience_CurrentlyWorkingWithEndDateThrows() {
        ExperienceRequest request = new ExperienceRequest(
                "Developer", "Acme Corp", EmploymentType.FULL_TIME,
                "Remote", LocalDate.of(2022, 1, 1), LocalDate.of(2024, 1, 1),
                true, "Invalid"
        );

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));

        assertThatThrownBy(() -> experienceService.addExperience(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("End date must be null when currently working");
    }

    @Test
    @DisplayName("deleteExperience deletes record successfully")
    void deleteExperience_Success() {
        WorkExperience exp = new WorkExperience(testProfile, "Intern", "Startup Ltd",
                EmploymentType.INTERNSHIP, "New York", LocalDate.of(2020, 6, 1),
                LocalDate.of(2020, 8, 31), false, "Internship");
        exp.setId(experienceId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(experienceRepository.findByIdAndProfileId(experienceId, testProfile.getId())).thenReturn(Optional.of(exp));

        experienceService.deleteExperience(userId, experienceId);

        verify(experienceRepository).delete(exp);
    }
}

