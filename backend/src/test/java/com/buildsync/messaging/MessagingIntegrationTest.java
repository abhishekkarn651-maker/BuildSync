package com.buildsync.messaging;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RefreshTokenRepository;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.collaboration.domain.ProjectMember;
import com.buildsync.collaboration.domain.ProjectMemberRole;
import com.buildsync.collaboration.infrastructure.CollaborationRequestRepository;
import com.buildsync.collaboration.infrastructure.ProjectMemberRepository;
import com.buildsync.messaging.api.CreateDirectConversationRequest;
import com.buildsync.messaging.api.SendMessagePayload;
import com.buildsync.messaging.domain.Conversation;
import com.buildsync.messaging.domain.ConversationType;
import com.buildsync.messaging.infrastructure.ConversationParticipantRepository;
import com.buildsync.messaging.infrastructure.ConversationRepository;
import com.buildsync.messaging.infrastructure.MessageRepository;
import com.buildsync.messaging.websocket.StompAuthChannelInterceptor;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MessagingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationParticipantRepository participantRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private CollaborationRequestRepository collaborationRequestRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ProjectTechnologyRepository projectTechnologyRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private DeveloperProfileSkillRepository profileSkillRepository;

    @Autowired
    private EducationRepository educationRepository;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private DeveloperProfileRepository profileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StompAuthChannelInterceptor stompAuthChannelInterceptor;

    @Autowired(required = false)
    private com.buildsync.github.infrastructure.ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository;

    private User alice;
    private User bob;
    private User charlie;

    private String aliceToken;
    private String bobToken;
    private String charlieToken;

    private Project publicProject;

    @BeforeEach
    void setUp() {
        tearDown();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        String encodedPw = passwordEncoder.encode("Password123!");

        // Alice
        alice = new User("Alice Developer", "alice_dev", "alice@messaging.com", encodedPw);
        alice.setRoles(Collections.singleton(userRole));
        alice = userRepository.save(alice);
        DeveloperProfile pAlice = new DeveloperProfile(alice);
        pAlice.setHeadline("Full Stack Lead");
        profileRepository.save(pAlice);
        aliceToken = jwtService.generateAccessToken(alice);

        // Bob
        bob = new User("Bob Builder", "bob_dev", "bob@messaging.com", encodedPw);
        bob.setRoles(Collections.singleton(userRole));
        bob = userRepository.save(bob);
        DeveloperProfile pBob = new DeveloperProfile(bob);
        pBob.setHeadline("Cloud Systems Specialist");
        profileRepository.save(pBob);
        bobToken = jwtService.generateAccessToken(bob);

        // Charlie
        charlie = new User("Charlie Outsider", "charlie_dev", "charlie@messaging.com", encodedPw);
        charlie.setRoles(Collections.singleton(userRole));
        charlie = userRepository.save(charlie);
        DeveloperProfile pCharlie = new DeveloperProfile(charlie);
        pCharlie.setHeadline("Frontend Specialist");
        profileRepository.save(pCharlie);
        charlieToken = jwtService.generateAccessToken(charlie);

        // Public project owned by Alice
        publicProject = new Project(alice, "Distributed Cloud Platform", "dist-cloud-platform",
                "High performance orchestration engine", "Specs",
                ProjectCategory.DEVOPS, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC,
                null, null, null, null);
        publicProject = projectRepository.save(publicProject);
    }

    @AfterEach
    void tearDown() {
        if (projectGitHubRepositoryRepository != null) {
            projectGitHubRepositoryRepository.deleteAll();
        }
        messageRepository.deleteAll();
        participantRepository.deleteAll();
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

    // =========================================================================
    // 1. DIRECT CONVERSATIONS
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/conversations/direct - Alice starts direct conversation with Bob")
    void createDirectConversation_Success() throws Exception {
        CreateDirectConversationRequest request = new CreateDirectConversationRequest("bob_dev");

        mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.conversationType").value("DIRECT"))
                .andExpect(jsonPath("$.otherParticipant.username").value("bob_dev"))
                .andExpect(jsonPath("$.otherParticipant.fullName").value("Bob Builder"));

        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(participantRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/v1/conversations/direct - Duplicate request returns existing conversation")
    void getOrCreateDirectConversation_ReturnsExisting() throws Exception {
        CreateDirectConversationRequest request = new CreateDirectConversationRequest("bob_dev");

        // First call
        String response1 = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Second call from Bob to Alice
        CreateDirectConversationRequest reverseRequest = new CreateDirectConversationRequest("alice_dev");
        String response2 = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reverseRequest)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String id1 = objectMapper.readTree(response1).get("id").asText();
        String id2 = objectMapper.readTree(response2).get("id").asText();

        assertThat(id1).isEqualTo(id2);
        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/conversations/direct - Cannot start conversation with self")
    void createDirectConversation_Self_ReturnsBadRequest() throws Exception {
        CreateDirectConversationRequest request = new CreateDirectConversationRequest("alice_dev");

        mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/conversations/direct - User not found returns 404")
    void createDirectConversation_UserNotFound_ReturnsNotFound() throws Exception {
        CreateDirectConversationRequest request = new CreateDirectConversationRequest("non_existent_user");

        mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 2. PROJECT CONVERSATIONS
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/conversation - Owner accesses/creates project chat")
    void getOrCreateProjectConversation_Owner_Success() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId() + "/conversation")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.conversationType").value("PROJECT"))
                .andExpect(jsonPath("$.project.id").value(publicProject.getId().toString()))
                .andExpect(jsonPath("$.project.title").value("Distributed Cloud Platform"));

        assertThat(conversationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/conversation - Member accesses project chat")
    void getOrCreateProjectConversation_Member_Success() throws Exception {
        // Add Bob as active project member
        projectMemberRepository.save(new ProjectMember(publicProject, bob, ProjectMemberRole.MEMBER));

        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId() + "/conversation")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationType").value("PROJECT"));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/conversation - Non-member forbidden (403)")
    void getOrCreateProjectConversation_NonMember_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId() + "/conversation")
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Removed member loses access to project conversation")
    void removedMember_LosesAccessToProjectConversation() throws Exception {
        ProjectMember member = projectMemberRepository.save(new ProjectMember(publicProject, bob, ProjectMemberRole.MEMBER));

        // Bob accesses successfully while a member
        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId() + "/conversation")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk());

        // Now remove Bob from project
        projectMemberRepository.delete(member);

        // Bob attempts to access again -> 403 Forbidden
        mockMvc.perform(get("/api/v1/projects/" + publicProject.getId() + "/conversation")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. CONVERSATION DETAILS & LIST
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/conversations - Returns paginated user conversations")
    void getUserConversations_Success() throws Exception {
        // Start a direct conversation with Bob
        mockMvc.perform(post("/api/v1/conversations/direct")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))));

        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].conversationType").value("DIRECT"));
    }

    @Test
    @DisplayName("GET /api/v1/conversations/{id} - Non-participant forbidden (403)")
    void getConversationDetail_NonParticipant_Forbidden() throws Exception {
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();

        String convId = objectMapper.readTree(res).get("id").asText();

        // Charlie tries to view Alice-Bob conversation
        mockMvc.perform(get("/api/v1/conversations/" + convId)
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 4. MESSAGES (PERSISTENCE, HISTORY, VALIDATION)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/conversations/{id}/messages - Persist and send message")
    void sendMessage_Success() throws Exception {
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();
        String convId = objectMapper.readTree(res).get("id").asText();

        SendMessagePayload payload = new SendMessagePayload("Hey Bob, let's sync on the distributed queue design.");

        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.content").value("Hey Bob, let's sync on the distributed queue design."))
                .andExpect(jsonPath("$.sender.username").value("alice_dev"));

        assertThat(messageRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/v1/conversations/{id}/messages - Blank content returns 400 Bad Request")
    void sendMessage_BlankContent_ReturnsBadRequest() throws Exception {
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();
        String convId = objectMapper.readTree(res).get("id").asText();

        SendMessagePayload payload = new SendMessagePayload("   ");

        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/conversations/{id}/messages - Non-participant cannot send (403)")
    void sendMessage_NonParticipant_Forbidden() throws Exception {
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();
        String convId = objectMapper.readTree(res).get("id").asText();

        SendMessagePayload payload = new SendMessagePayload("Sneaking into chat");

        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/conversations/{id}/messages - Chronological order history")
    void getMessageHistory_ChronologicalOrder() throws Exception {
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();
        String convId = objectMapper.readTree(res).get("id").asText();

        // Alice sends message 1
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessagePayload("Message 1"))));

        // Bob sends message 2
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessagePayload("Message 2"))));

        // Get history
        mockMvc.perform(get("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].content").value("Message 1"))
                .andExpect(jsonPath("$.content[1].content").value("Message 2"));
    }

    // =========================================================================
    // 5. READ RECEIPTS & UNREAD COUNTS
    // =========================================================================

    @Test
    @DisplayName("Read receipts and unread counts update accurately")
    void readReceiptsAndUnreadCounts_Flow() throws Exception {
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();
        String convId = objectMapper.readTree(res).get("id").asText();

        // Alice sends message to Bob
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SendMessagePayload("Unread note for Bob"))));

        // Bob checks unread count -> should be 1
        mockMvc.perform(get("/api/v1/conversations/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnreadCount").value(1));

        // Bob marks as read
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/read")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());

        // Bob checks unread count again -> should be 0
        mockMvc.perform(get("/api/v1/conversations/unread-count")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUnreadCount").value(0));
    }

    // =========================================================================
    // 6. STOMP WEBSOCKET AUTHENTICATION & SUBSCRIPTION TESTS
    // =========================================================================

    @Test
    @DisplayName("STOMP CONNECT - Valid JWT token authenticates successfully")
    void stompConnect_ValidJwt_Authenticated() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + aliceToken);
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        Message<?> result = stompAuthChannelInterceptor.preSend(message, null);

        assertThat(result).isNotNull();
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertThat(resultAccessor.getUser()).isNotNull();
        assertThat(resultAccessor.getUser().getName()).isEqualTo("alice_dev");
    }

    @Test
    @DisplayName("STOMP CONNECT - Missing or invalid token throws IllegalArgumentException")
    void stompConnect_InvalidJwt_Rejected() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid-garbage-token");
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> stompAuthChannelInterceptor.preSend(message, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("STOMP SUBSCRIBE - Unauthorized user subscription is rejected")
    void stompSubscribe_Unauthorized_Rejected() throws Exception {
        // Direct conversation between Alice and Bob
        String res = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("bob_dev"))))
                .andReturn().getResponse().getContentAsString();
        String convId = objectMapper.readTree(res).get("id").asText();

        // Charlie connects via STOMP
        StompHeaderAccessor connectAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectAccessor.setNativeHeader("Authorization", "Bearer " + charlieToken);
        connectAccessor.setLeaveMutable(true);
        Message<?> connectMsg = MessageBuilder.createMessage(new byte[0], connectAccessor.getMessageHeaders());
        Message<?> authedConnect = stompAuthChannelInterceptor.preSend(connectMsg, null);
        StompHeaderAccessor authedAccessor = StompHeaderAccessor.wrap(authedConnect);

        // Charlie attempts to SUBSCRIBE to /topic/conversations/{convId}
        StompHeaderAccessor subscribeAccessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribeAccessor.setUser(authedAccessor.getUser());
        subscribeAccessor.setDestination("/topic/conversations/" + convId);
        subscribeAccessor.setLeaveMutable(true);
        Message<?> subscribeMsg = MessageBuilder.createMessage(new byte[0], subscribeAccessor.getMessageHeaders());

        assertThatThrownBy(() -> stompAuthChannelInterceptor.preSend(subscribeMsg, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Direct conversation works between two registered accounts without developer profiles")
    void directChat_BetweenUsersWithoutDeveloperProfile_Success() throws Exception {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name()).orElseThrow();
        String encodedPw = passwordEncoder.encode("Password123!");

        // User A without profile
        User userA = new User("User Alpha", "user_alpha", "alpha@test.com", encodedPw);
        userA.setRoles(Collections.singleton(userRole));
        userA = userRepository.save(userA);
        String tokenA = jwtService.generateAccessToken(userA);

        // User B without profile
        User userB = new User("User Beta", "user_beta", "beta@test.com", encodedPw);
        userB.setRoles(Collections.singleton(userRole));
        userB = userRepository.save(userB);
        String tokenB = jwtService.generateAccessToken(userB);

        // Verify neither has a profile row in profileRepository
        assertThat(profileRepository.findByUserId(userA.getId())).isEmpty();
        assertThat(profileRepository.findByUserId(userB.getId())).isEmpty();

        // User A initiates direct chat with user B
        String createRes = mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("user_beta"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.conversationType").value("DIRECT"))
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.otherParticipant.username").value("user_beta"))
                .andExpect(jsonPath("$.otherParticipant.fullName").value("User Beta"))
                .andExpect(jsonPath("$.otherParticipant.userId").value(userB.getId().toString()))
                .andExpect(jsonPath("$.otherParticipant.id").value(userB.getId().toString()))
                .andExpect(jsonPath("$.otherParticipant.avatarUrl").isEmpty())
                .andExpect(jsonPath("$.directParticipant.username").value("user_beta"))
                .andExpect(jsonPath("$.directParticipant.userId").value(userB.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        String convId = objectMapper.readTree(createRes).get("id").asText();

        // User A sends message to User B
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessagePayload("Hello Beta, direct chat works!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.content").value("Hello Beta, direct chat works!"))
                .andExpect(jsonPath("$.sender.userId").value(userA.getId().toString()))
                .andExpect(jsonPath("$.sender.id").value(userA.getId().toString()))
                .andExpect(jsonPath("$.sender.username").value("user_alpha"))
                .andExpect(jsonPath("$.sender.avatarUrl").isEmpty());

        // User B views message history
        mockMvc.perform(get("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].content").value("Hello Beta, direct chat works!"))
                .andExpect(jsonPath("$.content[0].sender.userId").value(userA.getId().toString()))
                .andExpect(jsonPath("$.content[0].sender.id").value(userA.getId().toString()));

        // User B sends reply
        mockMvc.perform(post("/api/v1/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessagePayload("Hey Alpha, confirmed!"))))
                .andExpect(status().isCreated());

        // User A refreshes page and fetches conversation list
        mockMvc.perform(get("/api/v1/conversations")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(convId))
                .andExpect(jsonPath("$.content[0].conversationType").value("DIRECT"))
                .andExpect(jsonPath("$.content[0].type").value("DIRECT"))
                .andExpect(jsonPath("$.content[0].lastMessage.content").value("Hey Alpha, confirmed!"))
                .andExpect(jsonPath("$.content[0].lastMessage.senderUsername").value("user_beta"))
                .andExpect(jsonPath("$.content[0].unreadCount").value(1));

        // User A views detail
        mockMvc.perform(get("/api/v1/conversations/" + convId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationType").value("DIRECT"))
                .andExpect(jsonPath("$.type").value("DIRECT"))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andExpect(jsonPath("$.participants[0].userId").isNotEmpty());
    }

    @Test
    @DisplayName("Participant with blank or null full name falls back to username")
    void participant_WithBlankFullName_FallsBackToUsername() throws Exception {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name()).orElseThrow();
        String encodedPw = passwordEncoder.encode("Password123!");

        User noNameUser = new User("   ", "noname_dev", "noname@test.com", encodedPw);
        noNameUser.setRoles(Collections.singleton(userRole));
        noNameUser = userRepository.save(noNameUser);

        mockMvc.perform(post("/api/v1/conversations/direct")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDirectConversationRequest("noname_dev"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherParticipant.username").value("noname_dev"))
                .andExpect(jsonPath("$.otherParticipant.fullName").value("noname_dev"))
                .andExpect(jsonPath("$.directParticipant.fullName").value("noname_dev"));
    }
}
