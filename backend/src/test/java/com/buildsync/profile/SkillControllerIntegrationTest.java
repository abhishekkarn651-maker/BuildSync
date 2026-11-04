package com.buildsync.profile;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.api.AddSkillRequest;
import com.buildsync.profile.api.UpdateSkillProficiencyRequest;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.ProficiencyLevel;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
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
class SkillControllerIntegrationTest {

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
    private com.buildsync.profile.infrastructure.EducationRepository educationRepository;

    @Autowired
    private com.buildsync.profile.infrastructure.ExperienceRepository experienceRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private DeveloperProfile testProfile;
    private Skill javaSkill;
    private String jwtToken;

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

        testUser = new User("Skill Tester", "skilltester", "skilltester@buildsync.dev", passwordEncoder.encode("Password123!"));
        testUser.setRoles(Collections.singleton(userRole));
        testUser = userRepository.save(testUser);

        testProfile = new DeveloperProfile(testUser);
        testProfile.setHeadline("Software Architect");
        testProfile = profileRepository.save(testProfile);

        javaSkill = skillRepository.save(new Skill("Java", SkillCategory.PROGRAMMING_LANGUAGE));
        skillRepository.save(new Skill("Spring Boot", SkillCategory.FRAMEWORK));

        jwtToken = jwtService.generateAccessToken(testUser);
    }

    @Test
    @DisplayName("GET /api/v1/skills is public and returns list of skills")
    void searchSkills_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/skills"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/skills with search query filters skills")
    void searchSkills_WithQuery() throws Exception {
        mockMvc.perform(get("/api/v1/skills").param("search", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/skills assigns skill with authentication")
    void addSkill_Success() throws Exception {
        AddSkillRequest request = new AddSkillRequest(javaSkill.getId(), ProficiencyLevel.EXPERT);

        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skillId").value(javaSkill.getId().toString()))
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.proficiencyLevel").value("EXPERT"));

        assertThat(profileSkillRepository.existsByProfileIdAndSkillId(testProfile.getId(), javaSkill.getId())).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/skills without auth returns 401 Unauthorized")
    void addSkill_Unauthorized() throws Exception {
        AddSkillRequest request = new AddSkillRequest(javaSkill.getId(), ProficiencyLevel.EXPERT);

        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/profiles/me/skills returns 409 Conflict on duplicate skill")
    void addSkill_DuplicateConflict() throws Exception {
        AddSkillRequest request = new AddSkillRequest(javaSkill.getId(), ProficiencyLevel.EXPERT);

        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /api/v1/profiles/me/skills/{skillId} updates proficiency level")
    void updateProficiency_Success() throws Exception {
        AddSkillRequest addReq = new AddSkillRequest(javaSkill.getId(), ProficiencyLevel.BEGINNER);
        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        UpdateSkillProficiencyRequest updateReq = new UpdateSkillProficiencyRequest(ProficiencyLevel.ADVANCED);
        mockMvc.perform(put("/api/v1/profiles/me/skills/" + javaSkill.getId())
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proficiencyLevel").value("ADVANCED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/profiles/me/skills/{skillId} removes skill assignment")
    void deleteSkill_Success() throws Exception {
        AddSkillRequest addReq = new AddSkillRequest(javaSkill.getId(), ProficiencyLevel.BEGINNER);
        mockMvc.perform(post("/api/v1/profiles/me/skills")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/profiles/me/skills/" + javaSkill.getId())
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());

        assertThat(profileSkillRepository.existsByProfileIdAndSkillId(testProfile.getId(), javaSkill.getId())).isFalse();
    }
}
