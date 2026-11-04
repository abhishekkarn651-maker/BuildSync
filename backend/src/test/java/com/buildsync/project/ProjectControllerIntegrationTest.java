package com.buildsync.project;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.domain.Skill;
import com.buildsync.profile.domain.SkillCategory;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
import com.buildsync.project.api.CreateProjectRequest;
import com.buildsync.project.api.UpdateProjectRequest;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectTechnology;
import com.buildsync.project.domain.ProjectVisibility;
import com.buildsync.project.infrastructure.ProjectRepository;
import com.buildsync.project.infrastructure.ProjectTechnologyRepository;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectTechnologyRepository projectTechnologyRepository;

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
    private com.buildsync.collaboration.infrastructure.CollaborationRequestRepository collaborationRequestRepository;

    @Autowired
    private com.buildsync.collaboration.infrastructure.ProjectMemberRepository projectMemberRepository;

    @Autowired
    private com.buildsync.messaging.infrastructure.MessageRepository messageRepository;

    @Autowired
    private com.buildsync.messaging.infrastructure.ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private com.buildsync.messaging.infrastructure.ConversationRepository conversationRepository;

    @Autowired(required = false)
    private com.buildsync.github.infrastructure.ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User ownerUser;
    private User otherUser;
    private String ownerToken;
    private String otherToken;

    private Skill reactSkill;
    private Skill springSkill;
    private Skill postgresSkill;

    @BeforeEach
    void setUp() {
        if (projectGitHubRepositoryRepository != null) {
            projectGitHubRepositoryRepository.deleteAll();
        }
        messageRepository.deleteAll();
        conversationParticipantRepository.deleteAll();
        conversationRepository.deleteAll();
        collaborationRequestRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectTechnologyRepository.deleteAll();
        projectRepository.deleteAll();
        profileSkillRepository.deleteAll();
        educationRepository.deleteAll();
        experienceRepository.deleteAll();
        profileRepository.deleteAll();
        skillRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        // Seed Skills
        reactSkill = skillRepository.save(new Skill("React", SkillCategory.FRAMEWORK));
        springSkill = skillRepository.save(new Skill("Spring Boot", SkillCategory.FRAMEWORK));
        postgresSkill = skillRepository.save(new Skill("PostgreSQL", SkillCategory.DATABASE));

        // Create Project Owner
        ownerUser = new User("Alice ProjectOwner", "alice_owner", "alice@project.com", passwordEncoder.encode("Password123!"));
        ownerUser.setRoles(Collections.singleton(userRole));
        ownerUser = userRepository.save(ownerUser);

        DeveloperProfile ownerProfile = new DeveloperProfile(ownerUser);
        ownerProfile.setHeadline("Full Stack Lead");
        ownerProfile.setAvatarUrl("/api/v1/media/avatars/alice.png");
        profileRepository.save(ownerProfile);

        ownerToken = jwtService.generateAccessToken(ownerUser);

        // Create Another User (non-owner)
        otherUser = new User("Bob NonOwner", "bob_nonowner", "bob@project.com", passwordEncoder.encode("Password123!"));
        otherUser.setRoles(Collections.singleton(userRole));
        otherUser = userRepository.save(otherUser);

        otherToken = jwtService.generateAccessToken(otherUser);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        messageRepository.deleteAll();
        conversationParticipantRepository.deleteAll();
        conversationRepository.deleteAll();
        collaborationRequestRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectTechnologyRepository.deleteAll();
        projectRepository.deleteAll();
        profileSkillRepository.deleteAll();
        educationRepository.deleteAll();
        experienceRepository.deleteAll();
        profileRepository.deleteAll();
        skillRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/projects - Authenticated user creates project with technologies successfully")
    void createProject_Authenticated_Success() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("BuildSync Platform");
        request.setShortDescription("Developer collaboration and project showcase hub");
        request.setDetailedDescription("A comprehensive platform linking developers with real open source projects.");
        request.setCategory(ProjectCategory.FULL_STACK);
        request.setStatus(ProjectStatus.IN_PROGRESS);
        request.setVisibility(ProjectVisibility.PUBLIC);
        request.setGithubUrl("https://github.com/BuildSync/platform");
        request.setDemoUrl("https://buildsync.dev");
        request.setStartDate(LocalDate.of(2026, 1, 15));
        request.setCompletionDate(LocalDate.of(2026, 6, 30));
        request.setTechnologyIds(List.of(reactSkill.getId(), springSkill.getId()));

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.slug", containsString("buildsync-platform")))
                .andExpect(jsonPath("$.title", is("BuildSync Platform")))
                .andExpect(jsonPath("$.category", is("FULL_STACK")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.visibility", is("PUBLIC")))
                .andExpect(jsonPath("$.owner.id", is(ownerUser.getId().toString())))
                .andExpect(jsonPath("$.owner.username", is("alice_owner")))
                .andExpect(jsonPath("$.owner.fullName", is("Alice ProjectOwner")))
                .andExpect(jsonPath("$.owner.headline", is("Full Stack Lead")))
                .andExpect(jsonPath("$.technologies", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/v1/projects - Unauthenticated user rejected with 401 Unauthorized")
    void createProject_Unauthenticated_ReturnsUnauthorized() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("Anonymous Project");
        request.setShortDescription("Short description here");
        request.setCategory(ProjectCategory.BACKEND);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/projects - Missing required fields returns 400 Bad Request")
    void createProject_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        // Title and shortDescription missing

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("required")));
    }

    @Test
    @DisplayName("POST /api/v1/projects - Invalid technology ID returns 400 Bad Request")
    void createProject_InvalidTechnologyId_ReturnsBadRequest() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("Invalid Tech Project");
        request.setShortDescription("Testing missing skill handling");
        request.setCategory(ProjectCategory.AI_ML);
        request.setTechnologyIds(List.of(UUID.randomUUID()));

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("do not exist")));
    }

    @Test
    @DisplayName("POST /api/v1/projects - Completion date before start date returns 400 Bad Request")
    void createProject_InvalidDateRange_ReturnsBadRequest() throws Exception {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setTitle("Chronology Project");
        request.setShortDescription("Project with invalid completion date");
        request.setCategory(ProjectCategory.DEVOPS);
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setCompletionDate(LocalDate.of(2026, 4, 1)); // Earlier than start date

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Completion date cannot be before start date")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/mine - Authenticated owner retrieves their projects")
    void getMyProjects_ReturnsOnlyOwnedProjects() throws Exception {
        // Create 2 projects for owner
        Project p1 = new Project(ownerUser, "Owner Project 1", "owner-p1", "Desc 1", null,
                ProjectCategory.BACKEND, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC, null, null, null, null);
        Project p2 = new Project(ownerUser, "Owner Project 2", "owner-p2", "Desc 2", null,
                ProjectCategory.FRONTEND, ProjectStatus.PLANNING, ProjectVisibility.PRIVATE, null, null, null, null);
        projectRepository.saveAll(List.of(p1, p2));

        // Create 1 project for otherUser
        Project p3 = new Project(otherUser, "Other Project", "other-p", "Desc 3", null,
                ProjectCategory.MOBILE, ProjectStatus.IDEA, ProjectVisibility.PUBLIC, null, null, null, null);
        projectRepository.save(p3);

        mockMvc.perform(get("/api/v1/projects/mine")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].owner.username", is("alice_owner")))
                .andExpect(jsonPath("$[1].owner.username", is("alice_owner")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId} - Public project accessible anonymously")
    void getProject_PublicProject_AnonymousAccess() throws Exception {
        Project publicProject = new Project(ownerUser, "Public Showcase", "public-showcase",
                "A public open source initiative", "Details here",
                ProjectCategory.OPEN_SOURCE, ProjectStatus.COMPLETED, ProjectVisibility.PUBLIC,
                "https://github.com/owner/showcase", "https://showcase.dev", null, null);
        publicProject = projectRepository.save(publicProject);
        projectTechnologyRepository.save(new ProjectTechnology(publicProject, reactSkill));

        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(publicProject.getId().toString())))
                .andExpect(jsonPath("$.title", is("Public Showcase")))
                .andExpect(jsonPath("$.visibility", is("PUBLIC")))
                .andExpect(jsonPath("$.technologies", hasSize(1)))
                .andExpect(jsonPath("$.technologies[0].name", is("React")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId} - Private project accessible to owner")
    void getProject_PrivateProject_OwnerAccess_Allowed() throws Exception {
        Project privateProject = new Project(ownerUser, "Secret Internal Tool", "secret-tool",
                "Proprietary backend infrastructure", null,
                ProjectCategory.DEVOPS, ProjectStatus.PLANNING, ProjectVisibility.PRIVATE,
                null, null, null, null);
        privateProject = projectRepository.save(privateProject);

        mockMvc.perform(get("/api/v1/projects/" + privateProject.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Secret Internal Tool")))
                .andExpect(jsonPath("$.visibility", is("PRIVATE")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId} - Private project inaccessible to non-owner returns 404")
    void getProject_PrivateProject_NonOwnerOrAnonymous_ReturnsNotFound() throws Exception {
        Project privateProject = new Project(ownerUser, "Secret Internal Tool", "secret-tool",
                "Proprietary backend infrastructure", null,
                ProjectCategory.DEVOPS, ProjectStatus.PLANNING, ProjectVisibility.PRIVATE,
                null, null, null, null);
        privateProject = projectRepository.save(privateProject);

        // Anonymous attempt
        mockMvc.perform(get("/api/v1/projects/" + privateProject.getId()))
                .andExpect(status().isNotFound());

        // Other user attempt
        mockMvc.perform(get("/api/v1/projects/" + privateProject.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/projects/{slug} - Public project accessible by unique slug")
    void getProject_BySlug_Success() throws Exception {
        Project project = new Project(ownerUser, "Slug Project Test", "slug-test-123",
                "Short description here", null,
                ProjectCategory.FULL_STACK, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC,
                null, null, null, null);
        projectRepository.save(project);

        mockMvc.perform(get("/api/v1/projects/slug-test-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Slug Project Test")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId} - Owner updates project successfully")
    void updateProject_Owner_Success() throws Exception {
        Project project = new Project(ownerUser, "Initial Title", "initial-title",
                "Initial description", null,
                ProjectCategory.FRONTEND, ProjectStatus.IDEA, ProjectVisibility.PUBLIC,
                null, null, null, null);
        project = projectRepository.save(project);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest();
        updateRequest.setTitle("Updated Title");
        updateRequest.setShortDescription("Updated description");
        updateRequest.setDetailedDescription("Updated detailed overview");
        updateRequest.setCategory(ProjectCategory.BACKEND);
        updateRequest.setStatus(ProjectStatus.IN_PROGRESS);
        updateRequest.setVisibility(ProjectVisibility.PUBLIC);
        updateRequest.setGithubUrl("https://github.com/owner/updated");
        updateRequest.setTechnologyIds(List.of(postgresSkill.getId()));

        mockMvc.perform(put("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Title")))
                .andExpect(jsonPath("$.category", is("BACKEND")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.technologies", hasSize(1)))
                .andExpect(jsonPath("$.technologies[0].name", is("PostgreSQL")));
    }

    @Test
    @DisplayName("PUT /api/v1/projects/{projectId} - Non-owner attempt returns 403 Forbidden")
    void updateProject_NonOwner_ReturnsForbidden() throws Exception {
        Project project = new Project(ownerUser, "Owner Only Project", "owner-only",
                "Original description", null,
                ProjectCategory.FRONTEND, ProjectStatus.IDEA, ProjectVisibility.PUBLIC,
                null, null, null, null);
        project = projectRepository.save(project);

        UpdateProjectRequest updateRequest = new UpdateProjectRequest();
        updateRequest.setTitle("Malicious Modification");
        updateRequest.setShortDescription("Hacked description");
        updateRequest.setCategory(ProjectCategory.CYBERSECURITY);
        updateRequest.setStatus(ProjectStatus.COMPLETED);
        updateRequest.setVisibility(ProjectVisibility.PUBLIC);

        mockMvc.perform(put("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId} - Owner deletes project successfully")
    void deleteProject_Owner_Success() throws Exception {
        Project project = new Project(ownerUser, "To Be Deleted", "to-be-deleted",
                "Ephemeral description", null,
                ProjectCategory.OTHER, ProjectStatus.ARCHIVED, ProjectVisibility.PUBLIC,
                null, null, null, null);
        project = projectRepository.save(project);
        projectTechnologyRepository.save(new ProjectTechnology(project, reactSkill));

        mockMvc.perform(delete("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertFalse(projectRepository.existsById(project.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId} - Non-owner attempt returns 403 Forbidden")
    void deleteProject_NonOwner_ReturnsForbidden() throws Exception {
        Project project = new Project(ownerUser, "Protected Project", "protected-proj",
                "Owner protected", null,
                ProjectCategory.OTHER, ProjectStatus.ARCHIVED, ProjectVisibility.PUBLIC,
                null, null, null, null);
        project = projectRepository.save(project);

        mockMvc.perform(delete("/api/v1/projects/" + project.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/projects - Browse public projects with keyword, technology, category, and privacy enforcement")
    void browseProjects_FiltersAndPrivacy() throws Exception {
        // Project 1: React Full-Stack (Public)
        Project p1 = new Project(ownerUser, "E-Commerce Microservice", "ecommerce-microservice",
                "Scalable web store backend and frontend", "Detailed info",
                ProjectCategory.FULL_STACK, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC,
                null, null, null, null);
        p1 = projectRepository.save(p1);
        projectTechnologyRepository.save(new ProjectTechnology(p1, reactSkill));
        projectTechnologyRepository.save(new ProjectTechnology(p1, springSkill));

        // Project 2: AI Analytics (Public)
        Project p2 = new Project(otherUser, "Neural Code Reviewer", "neural-code-reviewer",
                "Deep learning models for automated PR review", "Detailed info",
                ProjectCategory.AI_ML, ProjectStatus.PLANNING, ProjectVisibility.PUBLIC,
                null, null, null, null);
        p2 = projectRepository.save(p2);
        projectTechnologyRepository.save(new ProjectTechnology(p2, postgresSkill));

        // Project 3: Internal Private Project (Private)
        Project p3 = new Project(ownerUser, "Secret Internal Blockchain", "secret-blockchain",
                "Confidential ledger implementation", "Secret details",
                ProjectCategory.BLOCKCHAIN, ProjectStatus.PLANNING, ProjectVisibility.PRIVATE,
                null, null, null, null);
        p3 = projectRepository.save(p3);
        projectTechnologyRepository.save(new ProjectTechnology(p3, springSkill));

        // 1. Browse All (should only return 2 public projects, p3 is hidden)
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));

        // 2. Filter by keyword "Neural"
        mockMvc.perform(get("/api/v1/projects?keyword=Neural"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Neural Code Reviewer")));

        // 3. Filter by category FULL_STACK
        mockMvc.perform(get("/api/v1/projects?category=FULL_STACK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("E-Commerce Microservice")));

        // 4. Filter by technology "Spring Boot"
        mockMvc.perform(get("/api/v1/projects?technology=Spring Boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("E-Commerce Microservice")));

        // 5. Keyword matching private project returns 0 results
        mockMvc.perform(get("/api/v1/projects?keyword=Blockchain"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/technologies - Returns project technologies")
    void getProjectTechnologies_Success() throws Exception {
        Project project = new Project(ownerUser, "Tech Stack Test", "tech-stack-test",
                "Testing technology association endpoint", null,
                ProjectCategory.FULL_STACK, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC,
                null, null, null, null);
        project = projectRepository.save(project);
        projectTechnologyRepository.save(new ProjectTechnology(project, reactSkill));
        projectTechnologyRepository.save(new ProjectTechnology(project, springSkill));

        mockMvc.perform(get("/api/v1/projects/" + project.getId() + "/technologies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}

