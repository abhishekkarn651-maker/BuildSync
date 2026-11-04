package com.buildsync.profile;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.api.AddSkillRequest;
import com.buildsync.profile.api.EducationRequest;
import com.buildsync.profile.api.ExperienceRequest;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.EmploymentType;
import com.buildsync.profile.domain.ProficiencyLevel;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExperienceControllerIntegrationTest {

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
    private SkillRepository skillRepository;

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
        skillRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        userA = new User("Carol Engineer", "caroldev", "carol@buildsync.dev", passwordEncoder.encode("Password123!"));
        userA.setRoles(Collections.singleton(userRole));
        userA = userRepository.save(userA);

        DeveloperProfile profileA = new DeveloperProfile(userA);
        profileA.setHeadline("Lead Cloud Architect");
        profileRepository.save(profileA);
        tokenA = jwtService.generateAccessToken(userA);

        userB = new User("Dave Engineer", "davedev", "dave@buildsync.dev", passwordEncoder.encode("Password123!"));
        userB.setRoles(Collections.singleton(userRole));
        userB = userRepository.save(userB);

        DeveloperProfile profileB = new DeveloperProfile(userB);
        profileRepository.save(profileB);
        tokenB = jwtService.generateAccessToken(userB);
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/experience creates work experience record")
    void addExperience_Success() throws Exception {
        ExperienceRequest request = new ExperienceRequest(
                "Senior Backend Engineer", "Acme Cloud", EmploymentType.FULL_TIME,
                "Seattle, WA", LocalDate.of(2021, 3, 1), null, true,
                "Designing scalable event-driven systems"
        );

        mockMvc.perform(post("/api/v1/profiles/me/experience")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.jobTitle").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.companyName").value("Acme Cloud"))
                .andExpect(jsonPath("$.currentlyWorking").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/experience validates date boundaries")
    void addExperience_InvalidDateOrder() throws Exception {
        ExperienceRequest request = new ExperienceRequest(
                "Backend Engineer", "Acme", EmploymentType.FULL_TIME,
                "Remote", LocalDate.of(2023, 1, 1), LocalDate.of(2021, 1, 1), false,
                "Invalid dates"
        );

        mockMvc.perform(post("/api/v1/profiles/me/experience")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Cross-user isolation: User B cannot delete User A's experience record")
    void crossUserIsolation() throws Exception {
        ExperienceRequest request = new ExperienceRequest(
                "Staff Engineer", "Cloud Corp", EmploymentType.FULL_TIME,
                "Remote", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), false,
                "Staff level"
        );

        String createRes = mockMvc.perform(post("/api/v1/profiles/me/experience")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String expId = objectMapper.readTree(createRes).get("id").asText();

        mockMvc.perform(delete("/api/v1/profiles/me/experience/" + expId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        assertThat(experienceRepository.findById(UUID.fromString(expId))).isPresent();
    }

    @Test
    @DisplayName("GET /api/v1/profiles/{username} returns profile with skills, education, and experience")
    void publicProfileIncludesDetailedSubEntities() throws Exception {
        // 1. Add skill
        Skill java = skillRepository.save(new Skill("Java", SkillCategory.PROGRAMMING_LANGUAGE));
        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddSkillRequest(java.getId(), ProficiencyLevel.EXPERT))))
                .andExpect(status().isCreated());

        // 2. Add education
        mockMvc.perform(post("/api/v1/profiles/me/education")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EducationRequest(
                                "Harvard", "M.S.", "CS", LocalDate.of(2018, 9, 1),
                                LocalDate.of(2020, 5, 1), false, "CS"
                        ))))
                .andExpect(status().isCreated());

        // 3. Add experience
        mockMvc.perform(post("/api/v1/profiles/me/experience")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ExperienceRequest(
                                "Tech Lead", "Meta", EmploymentType.FULL_TIME,
                                "Menlo Park, CA", LocalDate.of(2020, 6, 1),
                                null, true, "Leading architecture"
                        ))))
                .andExpect(status().isCreated());

        // 4. Query public profile without auth
        mockMvc.perform(get("/api/v1/profiles/caroldev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("caroldev"))
                .andExpect(jsonPath("$.headline").value("Lead Cloud Architect"))
                .andExpect(jsonPath("$.skills").isArray())
                .andExpect(jsonPath("$.skills.length()").value(1))
                .andExpect(jsonPath("$.skills[0].name").value("Java"))
                .andExpect(jsonPath("$.skills[0].proficiencyLevel").value("EXPERT"))
                .andExpect(jsonPath("$.education").isArray())
                .andExpect(jsonPath("$.education.length()").value(1))
                .andExpect(jsonPath("$.education[0].institution").value("Harvard"))
                .andExpect(jsonPath("$.experience").isArray())
                .andExpect(jsonPath("$.experience.length()").value(1))
                .andExpect(jsonPath("$.experience[0].companyName").value("Meta"))
                .andExpect(jsonPath("$.experience[0].currentlyWorking").value(true));
    }
}

