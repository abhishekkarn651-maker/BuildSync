package com.buildsync.profile;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.api.CreateProfileRequest;
import com.buildsync.profile.api.UpdateProfileRequest;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.ExperienceLevel;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeveloperProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeveloperProfileRepository profileRepository;

    @Autowired
    private com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository profileSkillRepository;

    @Autowired
    private com.buildsync.profile.infrastructure.EducationRepository educationRepository;

    @Autowired
    private com.buildsync.profile.infrastructure.ExperienceRepository experienceRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        profileSkillRepository.deleteAll();
        educationRepository.deleteAll();
        experienceRepository.deleteAll();
        profileRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        testUser = new User("Dev Test User", "devtester", "devtester@buildsync.dev", passwordEncoder.encode("Password123!"));
        testUser.setRoles(Collections.singleton(userRole));
        testUser = userRepository.save(testUser);

        jwtToken = jwtService.generateAccessToken(testUser);
    }

    @Test
    @DisplayName("POST /api/v1/profiles creates a profile and returns 201 Created")
    void createProfile_Success() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest(
                "Full Stack Engineer",
                "Building with Java & React",
                "Detailed developer journey...",
                "San Francisco, CA",
                "https://devtester.io",
                "https://github.com/devtester",
                "https://linkedin.com/in/devtester",
                "https://x.com/devtester",
                ExperienceLevel.MID_LEVEL,
                4,
                AvailabilityStatus.AVAILABLE
        );

        mockMvc.perform(post("/api/v1/profiles")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("devtester"))
                .andExpect(jsonPath("$.headline").value("Full Stack Engineer"))
                .andExpect(jsonPath("$.bio").value("Building with Java & React"))
                .andExpect(jsonPath("$.location").value("San Francisco, CA"))
                .andExpect(jsonPath("$.githubUrl").value("https://github.com/devtester"))
                .andExpect(jsonPath("$.experienceLevel").value("MID_LEVEL"))
                .andExpect(jsonPath("$.yearsOfExperience").value(4))
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));

        assertThat(profileRepository.findByUserId(testUser.getId())).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/profiles returns 401 Unauthorized without token")
    void createProfile_Unauthenticated() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest(
                "Headline", "Bio", "About", "Location",
                null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/profiles returns 409 Conflict when profile already exists")
    void createProfile_Conflict() throws Exception {
        DeveloperProfile existing = new DeveloperProfile(testUser);
        existing.setHeadline("Initial Profile");
        profileRepository.save(existing);

        CreateProfileRequest request = new CreateProfileRequest(
                "Second Profile", "Bio", "About", "Location",
                null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/profiles")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Developer profile already exists for user: devtester"));
    }

    @Test
    @DisplayName("POST /api/v1/profiles returns 400 Bad Request on invalid URL format")
    void createProfile_InvalidUrl() throws Exception {
        CreateProfileRequest request = new CreateProfileRequest(
                "Headline", "Bio", "About", "Location",
                "invalid-url-without-http", null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/profiles")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/profiles/me returns 200 OK with developer profile")
    void getCurrentProfile_Success() throws Exception {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setHeadline("Backend Specialist");
        profile.setLocation("Seattle, WA");
        profileRepository.save(profile);

        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("devtester"))
                .andExpect(jsonPath("$.headline").value("Backend Specialist"))
                .andExpect(jsonPath("$.location").value("Seattle, WA"));
    }

    @Test
    @DisplayName("GET /api/v1/profiles/me returns 404 Not Found when profile has not been created")
    void getCurrentProfile_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Developer profile not found for user id: " + testUser.getId()));
    }

    @Test
    @DisplayName("PUT /api/v1/profiles/me updates profile and returns 200 OK")
    void updateCurrentProfile_Success() throws Exception {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setHeadline("Old Headline");
        profile.setLocation("Old Location");
        profileRepository.save(profile);

        UpdateProfileRequest request = new UpdateProfileRequest(
                "Updated Senior Architect",
                "Updated Bio",
                "Updated About",
                "London, UK",
                "https://updated.dev",
                "https://github.com/devtester",
                "https://linkedin.com/in/devtester",
                null,
                ExperienceLevel.LEAD,
                12,
                AvailabilityStatus.MENTORING
        );

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Updated Senior Architect"))
                .andExpect(jsonPath("$.location").value("London, UK"))
                .andExpect(jsonPath("$.experienceLevel").value("LEAD"))
                .andExpect(jsonPath("$.yearsOfExperience").value(12))
                .andExpect(jsonPath("$.availabilityStatus").value("MENTORING"));
    }

    @Test
    @DisplayName("PUT /api/v1/profiles/me returns 404 Not Found when profile does not exist")
    void updateCurrentProfile_NotFound() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Headline", "Bio", "About", "Location",
                null, null, null, null, null, null, null
        );

        mockMvc.perform(put("/api/v1/profiles/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{username} is publicly accessible without token")
    void getProfileByUsername_Public() throws Exception {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setHeadline("Public Cloud Architect");
        profile.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        profileRepository.save(profile);

        mockMvc.perform(get("/api/v1/profiles/devtester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("devtester"))
                .andExpect(jsonPath("$.headline").value("Public Cloud Architect"))
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{username} supports case-insensitive username lookup")
    void getProfileByUsername_CaseInsensitive() throws Exception {
        DeveloperProfile profile = new DeveloperProfile(testUser);
        profile.setHeadline("Case Insensitive Dev");
        profileRepository.save(profile);

        mockMvc.perform(get("/api/v1/profiles/DevTester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("devtester"))
                .andExpect(jsonPath("$.headline").value("Case Insensitive Dev"));
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{username} returns 404 for non-existent username")
    void getProfileByUsername_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/nonexistentuser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with username: nonexistentuser"));
    }
}

