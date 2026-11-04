package com.buildsync.profile;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import com.buildsync.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AvatarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DeveloperProfileRepository profileRepository;

    @Autowired
    private DeveloperProfileSkillRepository profileSkillRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User testUser;
    private DeveloperProfile testProfile;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        profileSkillRepository.deleteAll();
        educationRepository.deleteAll();
        experienceRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        testUser = new User("Avatar Tester", "avatartester", "avatartester@buildsync.dev", passwordEncoder.encode("Password123!"));
        testUser.setRoles(Collections.singleton(userRole));
        testUser = userRepository.save(testUser);

        testProfile = new DeveloperProfile(testUser);
        testProfile.setHeadline("Avatar Specialist");
        testProfile = profileRepository.save(testProfile);

        jwtToken = jwtService.generateAccessToken(testUser);
    }

    private byte[] generateValidImageBytes(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/avatar successfully uploads valid JPEG and updates profile")
    void uploadAvatar_Success() throws Exception {
        byte[] imageBytes = generateValidImageBytes(120, 120, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.jpg", "image/jpeg", imageBytes);

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").isString())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("/api/v1/media/avatars/")));

        DeveloperProfile updated = profileRepository.findByUserId(testUser.getId()).orElseThrow();
        assertThat(updated.getAvatarUrl()).isNotNull();
        assertThat(updated.getAvatarUrl()).startsWith("/api/v1/media/avatars/");
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/avatar without authentication returns 401 Unauthorized")
    void uploadAvatar_Unauthorized() throws Exception {
        byte[] imageBytes = generateValidImageBytes(100, 100, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.jpg", "image/jpeg", imageBytes);

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/avatar with non-image file returns 400 Bad Request")
    void uploadAvatar_InvalidFileType() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", "Hello World".getBytes());

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar")
                        .file(textFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported content type")));
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/avatar with corrupted image returns 400 Bad Request")
    void uploadAvatar_CorruptedImage() throws Exception {
        MockMultipartFile corruptedFile = new MockMultipartFile(
                "file", "corrupted.jpg", "image/jpeg", "fake-corrupted-bytes".getBytes());

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar")
                        .file(corruptedFile)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid image signature")));
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/avatar replaces existing avatar cleanly")
    void uploadAvatar_Replacement_Success() throws Exception {
        byte[] firstImage = generateValidImageBytes(100, 100, "png");
        MockMultipartFile file1 = new MockMultipartFile(
                "file", "first.png", "image/png", firstImage);

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar")
                        .file(file1)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        DeveloperProfile firstSaved = profileRepository.findByUserId(testUser.getId()).orElseThrow();
        String firstAvatarUrl = firstSaved.getAvatarUrl();
        assertThat(firstAvatarUrl).isNotNull();

        byte[] secondImage = generateValidImageBytes(150, 150, "jpg");
        MockMultipartFile file2 = new MockMultipartFile(
                "file", "second.jpg", "image/jpeg", secondImage);

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar")
                        .file(file2)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        DeveloperProfile secondSaved = profileRepository.findByUserId(testUser.getId()).orElseThrow();
        assertThat(secondSaved.getAvatarUrl()).isNotNull();
        assertThat(secondSaved.getAvatarUrl()).isNotEqualTo(firstAvatarUrl);
    }

    @Test
    @DisplayName("DELETE /api/v1/profiles/me/avatar removes avatar and clears profile reference")
    void deleteAvatar_Success() throws Exception {
        byte[] imageBytes = generateValidImageBytes(100, 100, "jpg");
        MockMultipartFile file = new MockMultipartFile(
                "file", "pic.jpg", "image/jpeg", imageBytes);

        mockMvc.perform(multipart("/api/v1/profiles/me/avatar")
                        .file(file)
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/profiles/me/avatar")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());

        DeveloperProfile updated = profileRepository.findByUserId(testUser.getId()).orElseThrow();
        assertThat(updated.getAvatarUrl()).isNull();
    }

    @Test
    @DisplayName("DELETE /api/v1/profiles/me/avatar without authentication returns 401 Unauthorized")
    void deleteAvatar_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/profiles/me/avatar"))
                .andExpect(status().isUnauthorized());
    }
}

