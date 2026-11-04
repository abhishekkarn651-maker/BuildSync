package com.buildsync.profile;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.domain.AvailabilityStatus;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.DeveloperProfileSkill;
import com.buildsync.profile.domain.ExperienceLevel;
import com.buildsync.profile.domain.ProficiencyLevel;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeveloperSearchIntegrationTest {

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
    private SkillRepository skillRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User devAlice;
    private User devBob;
    private User devCharlie;

    private Skill javaSkill;
    private Skill reactSkill;
    private Skill pythonSkill;

    @BeforeEach
    void setUp() {
        profileSkillRepository.deleteAll();
        educationRepository.deleteAll();
        experienceRepository.deleteAll();
        profileRepository.deleteAll();
        skillRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        // Seed Skills
        javaSkill = skillRepository.save(new Skill("Java", SkillCategory.PROGRAMMING_LANGUAGE));
        reactSkill = skillRepository.save(new Skill("React", SkillCategory.FRAMEWORK));
        pythonSkill = skillRepository.save(new Skill("Python", SkillCategory.PROGRAMMING_LANGUAGE));

        // Developer 1: Alice Johnson
        devAlice = new User("Alice Johnson", "alice_j", "alice@secret.com", passwordEncoder.encode("Pass123!"));
        devAlice.setRoles(Collections.singleton(userRole));
        devAlice = userRepository.save(devAlice);

        DeveloperProfile profileAlice = new DeveloperProfile(devAlice);
        profileAlice.setHeadline("Staff Backend Architect");
        profileAlice.setBio("Specializing in high throughput microservices.");
        profileAlice.setLocation("San Francisco, CA");
        profileAlice.setExperienceLevel(ExperienceLevel.LEAD);
        profileAlice.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        profileAlice.setYearsOfExperience(8);
        profileAlice.setAvatarUrl("/api/v1/media/avatars/alice.png");
        profileAlice = profileRepository.save(profileAlice);
        profileSkillRepository.save(new DeveloperProfileSkill(profileAlice, javaSkill, ProficiencyLevel.EXPERT));

        // Developer 2: Bob Smith
        devBob = new User("Bob Smith", "bob_dev", "bob@secret.com", passwordEncoder.encode("Pass123!"));
        devBob.setRoles(Collections.singleton(userRole));
        devBob = userRepository.save(devBob);

        DeveloperProfile profileBob = new DeveloperProfile(devBob);
        profileBob.setHeadline("Frontend & UI Specialist");
        profileBob.setBio("Designing reactive user interfaces and design systems.");
        profileBob.setLocation("Bengaluru, India");
        profileBob.setExperienceLevel(ExperienceLevel.SENIOR);
        profileBob.setAvailabilityStatus(AvailabilityStatus.MENTORING);
        profileBob.setYearsOfExperience(5);
        profileBob.setAvatarUrl("/api/v1/media/avatars/bob.jpg");
        profileBob = profileRepository.save(profileBob);
        profileSkillRepository.save(new DeveloperProfileSkill(profileBob, reactSkill, ProficiencyLevel.ADVANCED));

        // Developer 3: Charlie Davis
        devCharlie = new User("Charlie Davis", "charlie_d", "charlie@secret.com", passwordEncoder.encode("Pass123!"));
        devCharlie.setRoles(Collections.singleton(userRole));
        devCharlie = userRepository.save(devCharlie);

        DeveloperProfile profileCharlie = new DeveloperProfile(devCharlie);
        profileCharlie.setHeadline("Data Platform Engineer");
        profileCharlie.setBio("Building ML inference pipelines.");
        profileCharlie.setLocation("London, UK");
        profileCharlie.setExperienceLevel(ExperienceLevel.JUNIOR);
        profileCharlie.setAvailabilityStatus(AvailabilityStatus.BUSY);
        profileCharlie.setYearsOfExperience(2);
        profileCharlie = profileRepository.save(profileCharlie);
        profileSkillRepository.save(new DeveloperProfileSkill(profileCharlie, pythonSkill, ProficiencyLevel.INTERMEDIATE));
    }

    @Test
    @DisplayName("GET /api/v1/profiles is accessible anonymously and returns paginated developers")
    void searchDevelopers_PublicAccess() throws Exception {
        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.page", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/profiles strictly excludes private email addresses")
    void searchDevelopers_ExcludesEmail() throws Exception {
        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").doesNotExist())
                .andExpect(jsonPath("$.content[1].email").doesNotExist())
                .andExpect(jsonPath("$.content[2].email").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/profiles?keyword=Alice filters by developer name")
    void searchDevelopers_ByName() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("keyword", "Alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fullName", is("Alice Johnson")))
                .andExpect(jsonPath("$.content[0].username", is("alice_j")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles?keyword=bob_dev filters by username")
    void searchDevelopers_ByUsername() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("keyword", "bob_dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("bob_dev")))
                .andExpect(jsonPath("$.content[0].fullName", is("Bob Smith")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles?skill=React filters developers by skill")
    void searchDevelopers_BySkill() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("skill", "React"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("bob_dev")))
                .andExpect(jsonPath("$.content[0].skills[0].name", is("React")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles?location=Bengaluru filters developers by location")
    void searchDevelopers_ByLocation() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("location", "Bengaluru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].location", containsString("Bengaluru")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles?experienceLevel=LEAD filters by experience level")
    void searchDevelopers_ByExperienceLevel() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("experienceLevel", "LEAD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].experienceLevel", is("LEAD")))
                .andExpect(jsonPath("$.content[0].fullName", is("Alice Johnson")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles?availability=MENTORING filters by availability status")
    void searchDevelopers_ByAvailability() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("availability", "MENTORING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].availabilityStatus", is("MENTORING")))
                .andExpect(jsonPath("$.content[0].username", is("bob_dev")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles with multiple combined filters returns accurate match")
    void searchDevelopers_CombinedFilters() throws Exception {
        mockMvc.perform(get("/api/v1/profiles")
                        .param("keyword", "microservices")
                        .param("skill", "Java")
                        .param("experienceLevel", "LEAD")
                        .param("availability", "AVAILABLE")
                        .param("location", "San Francisco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].username", is("alice_j")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles with pagination splits results correctly")
    void searchDevelopers_Pagination() throws Exception {
        mockMvc.perform(get("/api/v1/profiles")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.last", is(false)));

        // Page 1
        mockMvc.perform(get("/api/v1/profiles")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "name_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    @DisplayName("GET /api/v1/profiles with sort=name_asc sorts alphabetically")
    void searchDevelopers_Sorting() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("sort", "name_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName", is("Alice Johnson")))
                .andExpect(jsonPath("$.content[1].fullName", is("Bob Smith")))
                .andExpect(jsonPath("$.content[2].fullName", is("Charlie Davis")));
    }

    @Test
    @DisplayName("GET /api/v1/profiles returns empty content when no developers match")
    void searchDevelopers_EmptyResults() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("keyword", "NonExistentKeywordXYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/profiles with invalid pagination returns 400 Bad Request")
    void searchDevelopers_InvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/profiles").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page index must not be less than zero")));

        mockMvc.perform(get("/api/v1/profiles").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must not be less than one")));

        mockMvc.perform(get("/api/v1/profiles").param("size", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Page size must not exceed 50")));
    }
}

