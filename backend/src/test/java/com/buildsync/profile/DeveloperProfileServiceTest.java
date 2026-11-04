package com.buildsync.profile;

import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.common.exception.DuplicateResourceException;
import com.buildsync.common.exception.ResourceNotFoundException;
import com.buildsync.profile.api.CreateProfileRequest;
import com.buildsync.profile.api.ProfileResponse;
import com.buildsync.profile.api.UpdateProfileRequest;
import com.buildsync.profile.application.DeveloperProfileService;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.ExperienceLevel;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeveloperProfileServiceTest {

    @Mock
    private DeveloperProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository profileSkillRepository;

    @Mock
    private com.buildsync.profile.infrastructure.EducationRepository educationRepository;

    @Mock
    private com.buildsync.profile.infrastructure.ExperienceRepository experienceRepository;

    @Mock
    private com.buildsync.media.application.StorageService storageService;

    @Mock
    private com.buildsync.media.application.ImageValidator imageValidator;

    @InjectMocks
    private DeveloperProfileService profileService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = new User("Bharat Kumar", "bharatkumar", "bharat@example.com", "hashedPassword");
        testUser.setId(userId);
    }

    @Test
    @DisplayName("createProfile should successfully persist and return profile response")
    void createProfile_Success() {
        CreateProfileRequest request = new CreateProfileRequest(
                "Senior Architect",
                "Building cool software",
                "Detailed about me",
                "Bengaluru, India",
                "https://bharat.dev",
                "https://github.com/bharatkumar",
                "https://linkedin.com/in/bharatkumar",
                "https://x.com/bharatkumar",
                ExperienceLevel.SENIOR,
                8,
                AvailabilityStatus.AVAILABLE
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(profileRepository.save(any(DeveloperProfile.class))).thenAnswer(invocation -> {
            DeveloperProfile p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        ProfileResponse response = profileService.createProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo("bharatkumar");
        assertThat(response.getHeadline()).isEqualTo("Senior Architect");
        assertThat(response.getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);

        verify(profileRepository).save(any(DeveloperProfile.class));
    }

    @Test
    @DisplayName("createProfile should throw ResourceNotFoundException when user does not exist")
    void createProfile_UserNotFound() {
        CreateProfileRequest request = new CreateProfileRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.createProfile(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("createProfile should throw DuplicateResourceException when profile already exists")
    void createProfile_AlreadyExists() {
        CreateProfileRequest request = new CreateProfileRequest();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(profileRepository.existsByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> profileService.createProfile(userId, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("getProfileByUserId should return ProfileResponse when found")
    void getProfileByUserId_Success() {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setId(UUID.randomUUID());
        profile.setHeadline("Full Stack Engineer");

        when(profileRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getProfileByUserId(userId);

        assertThat(response).isNotNull();
        assertThat(response.getHeadline()).isEqualTo("Full Stack Engineer");
        assertThat(response.getUsername()).isEqualTo("bharatkumar");
    }

    @Test
    @DisplayName("getProfileByUserId should throw ResourceNotFoundException when not found")
    void getProfileByUserId_NotFound() {
        when(profileRepository.findByUserIdWithUser(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfileByUserId(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Developer profile not found");
    }

    @Test
    @DisplayName("updateProfileByUserId should update fields and return updated profile")
    void updateProfileByUserId_Success() {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setId(UUID.randomUUID());
        profile.setHeadline("Old Headline");

        UpdateProfileRequest request = new UpdateProfileRequest(
                "New Headline",
                "New Bio",
                "New About",
                "Berlin, Germany",
                "https://portfolio.dev",
                "https://github.com/bharat",
                null,
                null,
                ExperienceLevel.LEAD,
                10,
                AvailabilityStatus.BUSY
        );

        when(profileRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(DeveloperProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.updateProfileByUserId(userId, request);

        assertThat(response.getHeadline()).isEqualTo("New Headline");
        assertThat(response.getLocation()).isEqualTo("Berlin, Germany");
        assertThat(response.getExperienceLevel()).isEqualTo(ExperienceLevel.LEAD);
        assertThat(response.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BUSY);
    }

    @Test
    @DisplayName("getProfileByUsername should return ProfileResponse when found")
    void getProfileByUsername_Success() {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setId(UUID.randomUUID());
        profile.setHeadline("Staff Architect");

        when(profileRepository.findByUsernameWithUser("bharatkumar")).thenReturn(Optional.of(profile));

        ProfileResponse response = profileService.getProfileByUsername("bharatkumar");

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("bharatkumar");
        assertThat(response.getHeadline()).isEqualTo("Staff Architect");
    }

    @Test
    @DisplayName("getProfileByUsername should throw ResourceNotFoundException when user doesn't exist")
    void getProfileByUsername_UserNotFound() {
        when(profileRepository.findByUsernameWithUser("unknownuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("unknownuser")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfileByUsername("unknownuser"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with username");
    }

    @Test
    @DisplayName("getProfileByUsername should throw ResourceNotFoundException when user exists without profile")
    void getProfileByUsername_ProfileNotFound() {
        when(profileRepository.findByUsernameWithUser("bharatkumar")).thenReturn(Optional.empty());
        when(userRepository.findByUsernameIgnoreCase("bharatkumar")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> profileService.getProfileByUsername("bharatkumar"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Developer profile not found for username");
    }

    @Test
    @DisplayName("getProfileByUsername should throw IllegalArgumentException when username is empty")
    void getProfileByUsername_Empty() {
        assertThatThrownBy(() -> profileService.getProfileByUsername("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("uploadAvatar should validate, store image, update profile and delete old avatar")
    void uploadAvatar_Success() {
        DeveloperProfile existing = new DeveloperProfile(testUser);
        existing.setId(UUID.randomUUID());
        existing.setAvatarUrl("/api/v1/media/avatars/old-avatar.jpg");

        org.springframework.web.multipart.MultipartFile mockFile = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(storageService.storeAvatar(mockFile)).thenReturn("/api/v1/media/avatars/new-avatar.png");
        when(profileRepository.save(any(DeveloperProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.uploadAvatar(userId, mockFile);

        assertThat(response).isNotNull();
        assertThat(response.getAvatarUrl()).isEqualTo("/api/v1/media/avatars/new-avatar.png");
        verify(imageValidator).validate(mockFile);
        verify(storageService).storeAvatar(mockFile);
        verify(storageService).deleteAvatar("/api/v1/media/avatars/old-avatar.jpg");
    }

    @Test
    @DisplayName("deleteAvatar should remove avatarUrl and delete file from storage")
    void deleteAvatar_Success() {
        DeveloperProfile existing = new DeveloperProfile(testUser);
        existing.setId(UUID.randomUUID());
        existing.setAvatarUrl("/api/v1/media/avatars/current.jpg");

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(profileRepository.save(any(DeveloperProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResponse response = profileService.deleteAvatar(userId);

        assertThat(response).isNotNull();
        assertThat(response.getAvatarUrl()).isNull();
        verify(storageService).deleteAvatar("/api/v1/media/avatars/current.jpg");
    }

    @Test
    @DisplayName("deleteAvatar when no avatar exists should return without calling storage delete")
    void deleteAvatar_NoAvatar() {
        DeveloperProfile existing = new DeveloperProfile(testUser);
        existing.setId(UUID.randomUUID());
        existing.setAvatarUrl(null);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        ProfileResponse response = profileService.deleteAvatar(userId);

        assertThat(response).isNotNull();
        assertThat(response.getAvatarUrl()).isNull();
        org.mockito.Mockito.verifyNoInteractions(storageService);
    }
}

