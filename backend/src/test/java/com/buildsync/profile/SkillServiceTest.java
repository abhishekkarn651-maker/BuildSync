package com.buildsync.profile;

import com.buildsync.auth.domain.User;
import com.buildsync.common.exception.DuplicateResourceException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.AddSkillRequest;
import com.buildsync.profile.api.ProfileSkillResponse;
import com.buildsync.profile.api.SkillResponse;
import com.buildsync.profile.api.UpdateSkillProficiencyRequest;
import com.buildsync.profile.application.SkillService;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.DeveloperProfileSkill;
import com.buildsync.profile.domain.ProficiencyLevel;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private DeveloperProfileRepository profileRepository;

    @Mock
    private DeveloperProfileSkillRepository profileSkillRepository;

    @InjectMocks
    private SkillService skillService;

    private UUID userId;
    private DeveloperProfile testProfile;
    private Skill testSkill;
    private UUID skillId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = new User("Jane Dev", "janedev", "jane@buildsync.dev", "password");
        user.setId(userId);

        testProfile = new DeveloperProfile(user);
        testProfile.setId(UUID.randomUUID());

        skillId = UUID.randomUUID();
        testSkill = new Skill("Java", SkillCategory.PROGRAMMING_LANGUAGE);
        testSkill.setId(skillId);
    }

    @Test
    @DisplayName("searchSkills returns matching skills with query and category")
    void searchSkills_WithQueryAndCategory_Success() {
        when(skillRepository.findByCategoryAndNameContainingIgnoreCaseOrderByNameAsc(SkillCategory.PROGRAMMING_LANGUAGE, "java"))
                .thenReturn(Collections.singletonList(testSkill));

        List<SkillResponse> result = skillService.searchSkills("java", SkillCategory.PROGRAMMING_LANGUAGE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(0).getCategory()).isEqualTo(SkillCategory.PROGRAMMING_LANGUAGE);
    }

    @Test
    @DisplayName("searchSkills returns all skills when query and category are null")
    void searchSkills_All_Success() {
        when(skillRepository.findAllByOrderByNameAsc())
                .thenReturn(Collections.singletonList(testSkill));

        List<SkillResponse> result = skillService.searchSkills(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Java");
    }

    @Test
    @DisplayName("addSkillToProfile assigns skill successfully")
    void addSkillToProfile_Success() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(profileSkillRepository.existsByProfileIdAndSkillId(testProfile.getId(), skillId)).thenReturn(false);

        DeveloperProfileSkill savedSkill = new DeveloperProfileSkill(testProfile, testSkill, ProficiencyLevel.ADVANCED);
        savedSkill.setId(UUID.randomUUID());
        when(profileSkillRepository.save(any(DeveloperProfileSkill.class))).thenReturn(savedSkill);

        AddSkillRequest request = new AddSkillRequest(skillId, ProficiencyLevel.ADVANCED);
        ProfileSkillResponse response = skillService.addSkillToProfile(userId, request);

        assertThat(response.getName()).isEqualTo("Java");
        assertThat(response.getProficiencyLevel()).isEqualTo(ProficiencyLevel.ADVANCED);
    }

    @Test
    @DisplayName("addSkillToProfile throws DuplicateResourceException on duplicate assignment")
    void addSkillToProfile_DuplicateThrows() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(skillRepository.findById(skillId)).thenReturn(Optional.of(testSkill));
        when(profileSkillRepository.existsByProfileIdAndSkillId(testProfile.getId(), skillId)).thenReturn(true);

        AddSkillRequest request = new AddSkillRequest(skillId, ProficiencyLevel.ADVANCED);

        assertThatThrownBy(() -> skillService.addSkillToProfile(userId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Skill already assigned");
    }

    @Test
    @DisplayName("updateSkillProficiency updates proficiency level successfully")
    void updateSkillProficiency_Success() {
        DeveloperProfileSkill profileSkill = new DeveloperProfileSkill(testProfile, testSkill, ProficiencyLevel.BEGINNER);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(profileSkillRepository.findByProfileIdAndSkillId(testProfile.getId(), skillId))
                .thenReturn(Optional.of(profileSkill));
        when(profileSkillRepository.save(any(DeveloperProfileSkill.class))).thenReturn(profileSkill);

        UpdateSkillProficiencyRequest request = new UpdateSkillProficiencyRequest(ProficiencyLevel.EXPERT);
        ProfileSkillResponse response = skillService.updateSkillProficiency(userId, skillId, request);

        assertThat(response.getProficiencyLevel()).isEqualTo(ProficiencyLevel.EXPERT);
    }

    @Test
    @DisplayName("removeSkillFromProfile deletes skill from profile")
    void removeSkillFromProfile_Success() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(profileSkillRepository.existsByProfileIdAndSkillId(testProfile.getId(), skillId)).thenReturn(true);

        skillService.removeSkillFromProfile(userId, skillId);

        verify(profileSkillRepository).deleteByProfileIdAndSkillId(testProfile.getId(), skillId);
    }

    @Test
    @DisplayName("removeSkillFromProfile throws ResourceNotFoundException if skill not assigned")
    void removeSkillFromProfile_NotFoundThrows() {
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(testProfile));
        when(profileSkillRepository.existsByProfileIdAndSkillId(testProfile.getId(), skillId)).thenReturn(false);

        assertThatThrownBy(() -> skillService.removeSkillFromProfile(userId, skillId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not assigned to this profile");
    }
}
