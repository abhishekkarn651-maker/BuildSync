package com.buildsync.profile;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.api.EducationRequest;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
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

import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EducationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

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

        userA = new User("Alice Developer", "alicedev", "alice@buildsync.dev", passwordEncoder.encode("Password123!"));
        userA.setRoles(Collections.singleton(userRole));
        userA = userRepository.save(userA);

        DeveloperProfile profileA = new DeveloperProfile(userA);
        profileRepository.save(profileA);
        tokenA = jwtService.generateAccessToken(userA);

        userB = new User("Bob Developer", "bobdev", "bob@buildsync.dev", passwordEncoder.encode("Password123!"));
        userB.setRoles(Collections.singleton(userRole));
        userB = userRepository.save(userB);

        DeveloperProfile profileB = new DeveloperProfile(userB);
        profileRepository.save(profileB);
        tokenB = jwtService.generateAccessToken(userB);
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/education creates education record")
    void addEducation_Success() throws Exception {
        EducationRequest request = new EducationRequest(
                "UC Berkeley", "B.S.", "Electrical Engineering & Computer Science",
                LocalDate.of(2018, 8, 20), LocalDate.of(2022, 5, 15), false,
                "Studied computer systems and networking"
        );

        mockMvc.perform(post("/api/v1/profiles/me/education")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.institution").value("UC Berkeley"))
                .andExpect(jsonPath("$.degree").value("B.S."))
                .andExpect(jsonPath("$.fieldOfStudy").value("Electrical Engineering & Computer Science"));
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/education validates date order")
    void addEducation_InvalidDateOrder() throws Exception {
        EducationRequest request = new EducationRequest(
                "UC Berkeley", "B.S.", "EECS",
                LocalDate.of(2022, 5, 15), LocalDate.of(2018, 8, 20), false,
                "Invalid dates"
        );

        mockMvc.perform(post("/api/v1/profiles/me/education")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/profiles/me/education returns records for authenticated user")
    void getEducation_Success() throws Exception {
        EducationRequest request = new EducationRequest(
                "MIT", "M.Eng", "Computer Science",
                LocalDate.of(2022, 9, 1), null, true, "Graduate student"
        );

        mockMvc.perform(post("/api/v1/profiles/me/education")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/profiles/me/education")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].institution").value("MIT"))
                .andExpect(jsonPath("$[0].currentlyStudying").value(true));
    }

    @Test
    @DisplayName("Cross-user isolation: User B cannot update or delete User A's education record")
    void crossUserIsolation() throws Exception {
        EducationRequest createReq = new EducationRequest(
                "Harvard", "B.A.", "Computer Science",
                LocalDate.of(2016, 9, 1), LocalDate.of(2020, 5, 25), false, "CS degree"
        );

        String createRes = mockMvc.perform(post("/api/v1/profiles/me/education")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String eduId = objectMapper.readTree(createRes).get("id").asText();

        // Attempt update by User B
        EducationRequest updateReq = new EducationRequest(
                "Hacked", "B.A.", "CS",
                LocalDate.of(2016, 9, 1), LocalDate.of(2020, 5, 25), false, "Hacked"
        );

        mockMvc.perform(put("/api/v1/profiles/me/education/" + eduId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());

        // Attempt delete by User B
        mockMvc.perform(delete("/api/v1/profiles/me/education/" + eduId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        // Verify still exists for User A
        assertThat(educationRepository.findById(java.util.UUID.fromString(eduId))).isPresent();
    }
}

