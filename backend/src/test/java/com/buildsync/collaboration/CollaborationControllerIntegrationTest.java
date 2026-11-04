package com.buildsync.collaboration;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.collaboration.api.CreateInvitationRequest;
import com.buildsync.collaboration.api.CreateJoinRequest;
import com.buildsync.collaboration.domain.CollaborationRequest;
import com.buildsync.collaboration.domain.CollaborationRequestStatus;
import com.buildsync.collaboration.domain.CollaborationRequestType;
import com.buildsync.collaboration.domain.ProjectMember;
import com.buildsync.collaboration.domain.ProjectMemberRole;
import com.buildsync.collaboration.infrastructure.CollaborationRequestRepository;
import com.buildsync.collaboration.infrastructure.ProjectMemberRepository;
import com.buildsync.profile.domain.DeveloperProfile;
import com.buildsync.profile.infrastructure.DeveloperProfileRepository;
import com.buildsync.profile.infrastructure.DeveloperProfileSkillRepository;
import com.buildsync.profile.infrastructure.EducationRepository;
import com.buildsync.profile.infrastructure.ExperienceRepository;
import com.buildsync.profile.infrastructure.SkillRepository;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CollaborationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CollaborationRequestRepository collaborationRequestRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

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
    private com.buildsync.messaging.infrastructure.MessageRepository messageRepository;

    @Autowired
    private com.buildsync.messaging.infrastructure.ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private com.buildsync.messaging.infrastructure.ConversationRepository conversationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User ownerUser;
    private User devAlice;
    private User devBob;

    private String ownerToken;
    private String aliceToken;
    private String bobToken;

    private Project publicProject;
    private Project privateProject;

    @BeforeEach
    void setUp() {
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

        String encodedPw = passwordEncoder.encode("Password123!");

        // 1. Create Project Owner
        ownerUser = new User("Owner Dev", "owner_dev", "owner@buildsync.dev", encodedPw);
        ownerUser.setRoles(Collections.singleton(userRole));
        ownerUser = userRepository.save(ownerUser);
        DeveloperProfile pOwner = new DeveloperProfile(ownerUser);
        pOwner.setHeadline("Project Lead");
        profileRepository.save(pOwner);
        ownerToken = jwtService.generateAccessToken(ownerUser);

        // 2. Create Dev Alice
        devAlice = new User("Alice Wonder", "alice_wonder", "alice@buildsync.dev", encodedPw);
        devAlice.setRoles(Collections.singleton(userRole));
        devAlice = userRepository.save(devAlice);
        DeveloperProfile pAlice = new DeveloperProfile(devAlice);
        pAlice.setHeadline("Frontend Specialist");
        profileRepository.save(pAlice);
        aliceToken = jwtService.generateAccessToken(devAlice);

        // 3. Create Dev Bob
        devBob = new User("Bob Builder", "bob_builder", "bob@buildsync.dev", encodedPw);
        devBob.setRoles(Collections.singleton(userRole));
        devBob = userRepository.save(devBob);
        DeveloperProfile pBob = new DeveloperProfile(devBob);
        pBob.setHeadline("Backend Specialist");
        profileRepository.save(pBob);
        bobToken = jwtService.generateAccessToken(devBob);

        // 4. Create Public Project
        publicProject = new Project(ownerUser, "Open Cloud Platform", "open-cloud-platform",
                "Scalable open-source cloud orchestration", "Detailed specs",
                ProjectCategory.DEVOPS, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC,
                null, null, null, null);
        publicProject = projectRepository.save(publicProject);

        // 5. Create Private Project
        privateProject = new Project(ownerUser, "Secret Enterprise Tool", "secret-enterprise-tool",
                "Internal enterprise analytics", "Secret specs",
                ProjectCategory.FULL_STACK, ProjectStatus.PLANNING, ProjectVisibility.PRIVATE,
                null, null, null, null);
        privateProject = projectRepository.save(privateProject);
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
    @DisplayName("POST /api/v1/projects/{id}/join-requests - Developer requests to join public project successfully")
    void createJoinRequest_Success() throws Exception {
        CreateJoinRequest request = new CreateJoinRequest("I would love to help optimize Kubernetes configs.");

        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/join-requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.requestType", is("JOIN_REQUEST")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.project.id", is(publicProject.getId().toString())))
                .andExpect(jsonPath("$.requester.username", is("alice_wonder")))
                .andExpect(jsonPath("$.recipient.username", is("owner_dev")))
                .andExpect(jsonPath("$.message", is("I would love to help optimize Kubernetes configs.")));

        assertTrue(collaborationRequestRepository.existsByProjectIdAndRequesterIdAndStatus(
                publicProject.getId(), devAlice.getId(), CollaborationRequestStatus.PENDING));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/join-requests - Reject duplicate pending join request")
    void createJoinRequest_DuplicatePending_ReturnsBadRequest() throws Exception {
        CreateJoinRequest request = new CreateJoinRequest("First application");
        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/join-requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate second application
        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/join-requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("pending join request already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/join-requests - Owner cannot request to join their own project")
    void createJoinRequest_OwnerJoiningOwnProject_ReturnsBadRequest() throws Exception {
        CreateJoinRequest request = new CreateJoinRequest("Self application");
        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/join-requests")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Project owners cannot request to join their own project")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/join-requests - Join request to private project rejected")
    void createJoinRequest_PrivateProject_ReturnsBadRequest() throws Exception {
        CreateJoinRequest request = new CreateJoinRequest("Trying to join private");
        mockMvc.perform(post("/api/v1/projects/" + privateProject.getId() + "/join-requests")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("public projects")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/invitations - Project owner invites developer successfully")
    void createInvitation_Success() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest("alice_wonder", "Would you like to build our React UI?");

        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestType", is("INVITATION")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.requester.username", is("owner_dev")))
                .andExpect(jsonPath("$.recipient.username", is("alice_wonder")));

        assertTrue(collaborationRequestRepository.existsByProjectIdAndRecipientIdAndStatus(
                publicProject.getId(), devAlice.getId(), CollaborationRequestStatus.PENDING));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/invitations - Non-owner attempt returns 403 Forbidden")
    void createInvitation_NonOwner_ReturnsForbidden() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest("bob_builder", "Imposter invitation");

        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/invitations")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/invitations - Reject duplicate pending invitation")
    void createInvitation_DuplicatePending_ReturnsBadRequest() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest("alice_wonder", "Invite 1");
        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate second invitation
        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("pending invitation already exists")));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/invitations - Owner cannot invite themselves")
    void createInvitation_SelfInvite_ReturnsBadRequest() throws Exception {
        CreateInvitationRequest request = new CreateInvitationRequest("owner_dev", "Invite myself");

        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot invite yourself")));
    }

    @Test
    @DisplayName("POST /api/v1/collaborations/{requestId}/accept - Owner accepts join request -> creates ProjectMember")
    void acceptJoinRequest_Owner_CreatesMembership() throws Exception {
        // Alice applies
        CollaborationRequest joinRequest = new CollaborationRequest(publicProject, devAlice, ownerUser,
                CollaborationRequestType.JOIN_REQUEST, "Let me contribute");
        joinRequest = collaborationRequestRepository.save(joinRequest);

        // Owner accepts
        mockMvc.perform(post("/api/v1/collaborations/" + joinRequest.getId() + "/accept")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // Verify membership created
        assertTrue(projectMemberRepository.existsByProjectIdAndUserId(publicProject.getId(), devAlice.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/collaborations/{requestId}/accept - Non-owner cannot accept join request")
    void acceptJoinRequest_NonOwner_ReturnsForbidden() throws Exception {
        CollaborationRequest joinRequest = new CollaborationRequest(publicProject, devAlice, ownerUser,
                CollaborationRequestType.JOIN_REQUEST, "Let me contribute");
        joinRequest = collaborationRequestRepository.save(joinRequest);

        // Bob (non-owner) tries to accept Alice's join request
        mockMvc.perform(post("/api/v1/collaborations/" + joinRequest.getId() + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/collaborations/{requestId}/accept - Invitee accepts invitation -> creates ProjectMember")
    void acceptInvitation_Invitee_CreatesMembership() throws Exception {
        // Owner invites Bob
        CollaborationRequest invitation = new CollaborationRequest(publicProject, ownerUser, devBob,
                CollaborationRequestType.INVITATION, "Join our team");
        invitation = collaborationRequestRepository.save(invitation);

        // Bob accepts
        mockMvc.perform(post("/api/v1/collaborations/" + invitation.getId() + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // Verify membership created
        assertTrue(projectMemberRepository.existsByProjectIdAndUserId(publicProject.getId(), devBob.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/collaborations/{requestId}/accept - Wrong user cannot accept invitation")
    void acceptInvitation_WrongUser_ReturnsForbidden() throws Exception {
        CollaborationRequest invitation = new CollaborationRequest(publicProject, ownerUser, devBob,
                CollaborationRequestType.INVITATION, "Join our team");
        invitation = collaborationRequestRepository.save(invitation);

        // Alice tries to accept Bob's invitation
        mockMvc.perform(post("/api/v1/collaborations/" + invitation.getId() + "/accept")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/collaborations/{requestId}/reject - Owner rejects join request")
    void rejectJoinRequest_Owner_Success() throws Exception {
        CollaborationRequest joinRequest = new CollaborationRequest(publicProject, devAlice, ownerUser,
                CollaborationRequestType.JOIN_REQUEST, "Let me contribute");
        joinRequest = collaborationRequestRepository.save(joinRequest);

        mockMvc.perform(post("/api/v1/collaborations/" + joinRequest.getId() + "/reject")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        assertFalse(projectMemberRepository.existsByProjectIdAndUserId(publicProject.getId(), devAlice.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/collaborations/{requestId}/cancel - Requester cancels their join request")
    void cancelJoinRequest_Requester_Success() throws Exception {
        CollaborationRequest joinRequest = new CollaborationRequest(publicProject, devAlice, ownerUser,
                CollaborationRequestType.JOIN_REQUEST, "Let me contribute");
        joinRequest = collaborationRequestRepository.save(joinRequest);

        mockMvc.perform(post("/api/v1/collaborations/" + joinRequest.getId() + "/cancel")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id}/members - Public project team visible anonymously")
    void getProjectMembers_PublicProject_AnonymousAllowed() throws Exception {
        // Add Bob as a member
        projectMemberRepository.save(new ProjectMember(publicProject, devBob, ProjectMemberRole.MEMBER));

        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId() + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))) // Owner + Bob
                .andExpect(jsonPath("$[0].username", is("owner_dev")))
                .andExpect(jsonPath("$[0].role", is("OWNER")))
                .andExpect(jsonPath("$[0].owner", is(true)))
                .andExpect(jsonPath("$[1].username", is("bob_builder")))
                .andExpect(jsonPath("$[1].role", is("MEMBER")))
                .andExpect(jsonPath("$[1].owner", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id}/members - Private project team restricted from unauthorized users (404)")
    void getProjectMembers_PrivateProject_Unauthorized_ReturnsNotFound() throws Exception {
        // Anonymous visitor
        mockMvc.perform(get("/api/v1/projects/" + privateProject.getId() + "/members"))
                .andExpect(status().isNotFound());

        // Alice (non-member)
        mockMvc.perform(get("/api/v1/projects/" + privateProject.getId() + "/members")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNotFound());

        // Owner (authorized)
        mockMvc.perform(get("/api/v1/projects/" + privateProject.getId() + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("owner_dev")));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id}/members/{userId} - Owner removes member successfully")
    void removeMember_Owner_Success() throws Exception {
        projectMemberRepository.save(new ProjectMember(publicProject, devBob, ProjectMemberRole.MEMBER));
        assertTrue(projectMemberRepository.existsByProjectIdAndUserId(publicProject.getId(), devBob.getId()));

        mockMvc.perform(delete("/api/v1/projects/" + publicProject.getId() + "/members/" + devBob.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertFalse(projectMemberRepository.existsByProjectIdAndUserId(publicProject.getId(), devBob.getId()));
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{id}/members/{userId} - Non-owner attempt returns 403 Forbidden")
    void removeMember_NonOwner_ReturnsForbidden() throws Exception {
        projectMemberRepository.save(new ProjectMember(publicProject, devBob, ProjectMemberRole.MEMBER));

        mockMvc.perform(delete("/api/v1/projects/" + publicProject.getId() + "/members/" + devBob.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/leave - Member leaves project successfully")
    void leaveProject_Member_Success() throws Exception {
        projectMemberRepository.save(new ProjectMember(publicProject, devBob, ProjectMemberRole.MEMBER));

        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/leave")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());

        assertFalse(projectMemberRepository.existsByProjectIdAndUserId(publicProject.getId(), devBob.getId()));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/leave - Owner cannot leave their own project")
    void leaveProject_Owner_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/projects/" + publicProject.getId() + "/leave")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("owners cannot leave their own project")));
    }

    @Test
    @DisplayName("GET /api/v1/collaborations/my-teams - Authenticated developer retrieves active teams")
    void getMyTeams_Success() throws Exception {
        // Bob is a member of publicProject
        projectMemberRepository.save(new ProjectMember(publicProject, devBob, ProjectMemberRole.MEMBER));

        mockMvc.perform(get("/api/v1/collaborations/my-teams")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].project.title", is("Open Cloud Platform")))
                .andExpect(jsonPath("$[0].role", is("MEMBER")))
                .andExpect(jsonPath("$[0].owner", is(false)))
                .andExpect(jsonPath("$[0].memberCount", is(2))); // Owner + Bob
    }

    @Test
    @DisplayName("Concurrency & Duplicate safety: Cannot accept twice without error")
    void acceptTwice_Rejected() throws Exception {
        CollaborationRequest invitation = new CollaborationRequest(publicProject, ownerUser, devBob,
                CollaborationRequestType.INVITATION, "Join our team");
        invitation = collaborationRequestRepository.save(invitation);

        // 1. First accept succeeds
        mockMvc.perform(post("/api/v1/collaborations/" + invitation.getId() + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACCEPTED")));

        // 2. Second accept fails with 400 because status is already ACCEPTED
        mockMvc.perform(post("/api/v1/collaborations/" + invitation.getId() + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isBadRequest());
    }
}
