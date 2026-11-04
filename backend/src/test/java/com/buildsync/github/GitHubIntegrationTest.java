package com.buildsync.github;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import com.buildsync.auth.infrastructure.RoleRepository;
import com.buildsync.auth.infrastructure.UserRepository;
import com.buildsync.github.api.dto.LinkProjectRepositoryRequest;
import com.buildsync.github.api.dto.UpdateRepoShowcaseRequest;
import com.buildsync.github.client.GitHubApiClient;
import com.buildsync.github.client.GitHubOAuthTokenResponse;
import com.buildsync.github.client.GitHubRepoDto;
import com.buildsync.github.client.GitHubUserResponse;
import com.buildsync.github.domain.GitHubConnection;
import com.buildsync.github.domain.GitHubConnectionStatus;
import com.buildsync.github.domain.GitHubOAuthState;
import com.buildsync.github.domain.GitHubRepository;
import com.buildsync.github.domain.RepositoryVisibility;
import com.buildsync.github.infrastructure.GitHubConnectionRepository;
import com.buildsync.github.infrastructure.GitHubOAuthStateRepository;
import com.buildsync.github.infrastructure.GitHubRepositoryRepository;
import com.buildsync.github.infrastructure.ProjectGitHubRepositoryRepository;
import com.buildsync.github.security.PkceUtil;
import com.buildsync.github.security.TokenEncryptionService;
import com.buildsync.project.domain.Project;
import com.buildsync.project.domain.ProjectCategory;
import com.buildsync.project.domain.ProjectStatus;
import com.buildsync.project.domain.ProjectVisibility;
import com.buildsync.project.infrastructure.ProjectRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GitHubIntegrationTest.TestConfig.class)
class GitHubIntegrationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public GitHubApiClient testGitHubApiClient() {
            return new StubGitHubApiClient();
        }
    }

    public static class StubGitHubApiClient implements GitHubApiClient {
        public GitHubOAuthTokenResponse tokenResponse;
        public GitHubUserResponse userResponse;
        public List<GitHubRepoDto> repositories = new java.util.ArrayList<>();
        public int revokeCount = 0;

        @Override
        public GitHubOAuthTokenResponse exchangeAuthorizationCode(String code, String codeVerifier) {
            return tokenResponse != null ? tokenResponse : new GitHubOAuthTokenResponse("default_token", 3600L, "default_refresh", 7200L, "read:user,repo", "bearer", null, null);
        }

        @Override
        public GitHubOAuthTokenResponse refreshAccessToken(String refreshToken) {
            return tokenResponse != null ? tokenResponse : new GitHubOAuthTokenResponse("refreshed_token", 3600L, "new_refresh", 7200L, "read:user,repo", "bearer", null, null);
        }

        @Override
        public GitHubUserResponse getUserProfile(String accessToken) {
            return userResponse != null ? userResponse : new GitHubUserResponse(99999L, "default_user", "https://avatar", "https://github.com/default", "Name", "email@test.com");
        }

        @Override
        public List<GitHubRepoDto> getUserRepositories(String accessToken, int page, int perPage) {
            if (page == 1) {
                return repositories;
            }
            return Collections.emptyList();
        }

        @Override
        public void revokeAuthorization(String accessToken) {
            revokeCount++;
        }

        public void reset() {
            tokenResponse = null;
            userResponse = null;
            repositories = new java.util.ArrayList<>();
            revokeCount = 0;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubApiClient gitHubApiClient;

    private StubGitHubApiClient stubClient() {
        return (StubGitHubApiClient) gitHubApiClient;
    }

    @Autowired
    private TokenEncryptionService encryptionService;

    @Autowired
    private GitHubConnectionRepository connectionRepository;

    @Autowired
    private GitHubOAuthStateRepository oAuthStateRepository;

    @Autowired
    private GitHubRepositoryRepository gitHubRepositoryRepository;

    @Autowired
    private ProjectGitHubRepositoryRepository projectGitHubRepositoryRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User alice;
    private User bob;
    private String aliceToken;
    private String bobToken;
    private Project aliceProject;

    @BeforeEach
    void setUp() {
        tearDown();
        stubClient().reset();

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_USER.name())));

        String encodedPw = passwordEncoder.encode("Password123!");

        // Alice
        alice = new User("Alice GitHubber", "alice_gh", "alice@githubtest.com", encodedPw);
        alice.setRoles(Collections.singleton(userRole));
        alice = userRepository.save(alice);
        aliceToken = jwtService.generateAccessToken(alice);

        // Bob
        bob = new User("Bob Builder", "bob_gh", "bob@githubtest.com", encodedPw);
        bob.setRoles(Collections.singleton(userRole));
        bob = userRepository.save(bob);
        bobToken = jwtService.generateAccessToken(bob);

        // Alice's Project
        aliceProject = new Project(alice, "Alice BuildSync Tool", "alice-buildsync-tool",
                "Innovative development synchronization system", null,
                ProjectCategory.FULL_STACK, ProjectStatus.IN_PROGRESS, ProjectVisibility.PUBLIC,
                null, null, null, null);
        aliceProject = projectRepository.save(aliceProject);
    }

    @AfterEach
    void tearDown() {
        projectGitHubRepositoryRepository.deleteAll();
        gitHubRepositoryRepository.deleteAll();
        connectionRepository.deleteAll();
        oAuthStateRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    // =========================================================================
    // 1. CRYPTOGRAPHIC & PKCE TESTS
    // =========================================================================

    @Test
    @DisplayName("TokenEncryptionService - AES-GCM Encrypt & Decrypt Roundtrip")
    void tokenEncryption_EncryptDecryptRoundtrip() {
        String originalToken = "gho_test_user_access_token_1234567890abcdef";
        String encrypted = encryptionService.encrypt(originalToken);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(originalToken);

        String decrypted = encryptionService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(originalToken);
    }

    @Test
    @DisplayName("TokenEncryptionService - Unique IV Per Operation produces distinct ciphertexts")
    void tokenEncryption_UniqueIvPerOperation() {
        String token = "gho_static_access_token";
        String enc1 = encryptionService.encrypt(token);
        String enc2 = encryptionService.encrypt(token);

        assertThat(enc1).isNotEqualTo(enc2);
        assertThat(encryptionService.decrypt(enc1)).isEqualTo(token);
        assertThat(encryptionService.decrypt(enc2)).isEqualTo(token);
    }

    @Test
    @DisplayName("TokenEncryptionService - Tampered ciphertext throws SecurityException")
    void tokenEncryption_TamperedCiphertextFails() {
        String encrypted = encryptionService.encrypt("secret_token");
        // Tamper with base64 payload
        String tampered = encrypted.substring(0, encrypted.length() - 4) + "AAAA";

        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("PkceUtil - S256 Challenge computation is deterministic and RFC compliant")
    void pkceUtil_S256ChallengeComputation() {
        String verifier = PkceUtil.generateCodeVerifier();
        assertThat(verifier.length()).isGreaterThanOrEqualTo(43);

        String challenge1 = PkceUtil.computeCodeChallenge(verifier);
        String challenge2 = PkceUtil.computeCodeChallenge(verifier);

        assertThat(challenge1).isEqualTo(challenge2);
        assertThat(challenge1).doesNotContain("="); // Base64URL without padding
    }

    // =========================================================================
    // 2. OAUTH CONNECTION FLOW & CALLBACK
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/integrations/github/connect - Authenticated user initiates connection")
    void connect_Initiate_Success() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/github/connect")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").exists())
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("client_id=")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("code_challenge=")))
                .andExpect(jsonPath("$.authorizationUrl").value(org.hamcrest.Matchers.containsString("state=")));

        List<GitHubOAuthState> states = oAuthStateRepository.findAll();
        assertThat(states).hasSize(1);
        assertThat(states.get(0).getUser().getId()).isEqualTo(alice.getId());
        assertThat(states.get(0).isConsumed()).isFalse();
    }

    @Test
    @DisplayName("POST /api/v1/integrations/github/connect - Unauthenticated request fails with 401")
    void connect_Unauthenticated_Fails() throws Exception {
        mockMvc.perform(post("/api/v1/integrations/github/connect"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/integrations/github/callback - Successful OAuth code exchange and credential encryption")
    void callback_Success() throws Exception {
        // 1. Initiate state
        String rawState = PkceUtil.generateState();
        String stateHash = PkceUtil.hashState(rawState);
        String verifier = PkceUtil.generateCodeVerifier();
        GitHubOAuthState state = new GitHubOAuthState(stateHash, verifier, alice, Instant.now().plusSeconds(600));
        oAuthStateRepository.save(state);

        // 2. Mock GitHub API client responses
        stubClient().tokenResponse = new GitHubOAuthTokenResponse(
                "gho_sample_access_token_abc",
                28800L,
                "ghr_sample_refresh_token_xyz",
                15811200L,
                "read:user,repo",
                "bearer",
                null,
                null
        );

        stubClient().userResponse = new GitHubUserResponse(
                987654321L,
                "alice_octocat",
                "https://avatars.githubusercontent.com/u/987654321",
                "https://github.com/alice_octocat",
                "Alice Octocat",
                "alice@github.com"
        );

        // 3. Perform Callback
        mockMvc.perform(get("/api/v1/integrations/github/callback")
                        .param("code", "valid_github_auth_code")
                        .param("state", rawState))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("status=success")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("alice_octocat")));

        // 4. Verify DB state
        GitHubOAuthState consumedState = oAuthStateRepository.findByStateHash(stateHash).orElseThrow();
        assertThat(consumedState.isConsumed()).isTrue();
        assertThat(consumedState.getConsumedAt()).isNotNull();

        GitHubConnection connection = connectionRepository.findByUserId(alice.getId()).orElseThrow();
        assertThat(connection.getGithubUsername()).isEqualTo("alice_octocat");
        assertThat(connection.getGithubUserId()).isEqualTo(987654321L);
        assertThat(connection.getConnectionStatus()).isEqualTo(GitHubConnectionStatus.CONNECTED);

        // Tokens are encrypted at rest and decrypt cleanly
        assertThat(encryptionService.decrypt(connection.getEncryptedAccessToken())).isEqualTo("gho_sample_access_token_abc");
        assertThat(encryptionService.decrypt(connection.getEncryptedRefreshToken())).isEqualTo("ghr_sample_refresh_token_xyz");
    }

    @Test
    @DisplayName("GET /api/v1/integrations/github/callback - Replayed state is rejected")
    void callback_ReplayedState_Rejected() throws Exception {
        String rawState = PkceUtil.generateState();
        String stateHash = PkceUtil.hashState(rawState);
        GitHubOAuthState state = new GitHubOAuthState(stateHash, "verifier", alice, Instant.now().plusSeconds(600));
        state.markConsumed();
        oAuthStateRepository.save(state);

        mockMvc.perform(get("/api/v1/integrations/github/callback")
                        .param("code", "test_code")
                        .param("state", rawState))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("error=state_already_consumed")));
    }

    @Test
    @DisplayName("GET /api/v1/integrations/github/callback - Expired state is rejected")
    void callback_ExpiredState_Rejected() throws Exception {
        String rawState = PkceUtil.generateState();
        String stateHash = PkceUtil.hashState(rawState);
        GitHubOAuthState state = new GitHubOAuthState(stateHash, "verifier", alice, Instant.now().minusSeconds(10));
        oAuthStateRepository.save(state);

        mockMvc.perform(get("/api/v1/integrations/github/callback")
                        .param("code", "test_code")
                        .param("state", rawState))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("error=state_expired")));
    }

    @Test
    @DisplayName("GET /api/v1/integrations/github/callback - Duplicate GitHub account linking to different user is rejected")
    void callback_DuplicateGitHubAccount_Rejected() throws Exception {
        // Pre-link GitHub ID 99999 to Bob
        GitHubConnection bobConn = new GitHubConnection(
                bob, 99999L, "shared_github_user", "https://avatar", "https://profile",
                encryptionService.encrypt("bob_token"), null, Instant.now().plusSeconds(3600), null
        );
        connectionRepository.save(bobConn);

        // Alice attempts to link the same GitHub ID 99999
        String rawState = PkceUtil.generateState();
        String stateHash = PkceUtil.hashState(rawState);
        GitHubOAuthState state = new GitHubOAuthState(stateHash, "verifier", alice, Instant.now().plusSeconds(600));
        oAuthStateRepository.save(state);

        stubClient().tokenResponse = new GitHubOAuthTokenResponse("token", 3600L, null, null, "read:user", "bearer", null, null);
        stubClient().userResponse = new GitHubUserResponse(99999L, "shared_github_user", "https://avatar", "https://profile", "Shared", "shared@test.com");

        mockMvc.perform(get("/api/v1/integrations/github/callback")
                        .param("code", "code_alice")
                        .param("state", rawState))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("error=github_account_already_linked")));
    }

    // =========================================================================
    // 3. STATUS & DISCONNECT
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/integrations/github/status - Disconnected when no connection exists")
    void getStatus_Disconnected() throws Exception {
        mockMvc.perform(get("/api/v1/integrations/github/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected", is(false)))
                .andExpect(jsonPath("$.tokenStatus", is("DISCONNECTED")));
    }

    @Test
    @DisplayName("GET /api/v1/integrations/github/status - Connected with stats and no token leakage")
    void getStatus_Connected_NoTokenLeakage() throws Exception {
        GitHubConnection conn = new GitHubConnection(
                alice, 12345L, "alice_live", "https://avatar.url", "https://github.com/alice_live",
                encryptionService.encrypt("secret_access_token_never_expose"),
                encryptionService.encrypt("secret_refresh_token_never_expose"),
                Instant.now().plusSeconds(3600), Instant.now().plusSeconds(86400)
        );
        connectionRepository.save(conn);

        mockMvc.perform(get("/api/v1/integrations/github/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected", is(true)))
                .andExpect(jsonPath("$.githubUsername", is("alice_live")))
                .andExpect(jsonPath("$.tokenStatus", is("VALID")))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.encryptedAccessToken").doesNotExist());
    }

    @Test
    @DisplayName("DELETE /api/v1/integrations/github/disconnect - Disconnects and deletes credentials")
    void disconnect_Success() throws Exception {
        GitHubConnection conn = new GitHubConnection(
                alice, 12345L, "alice_to_disconnect", "https://avatar", "https://profile",
                encryptionService.encrypt("token"), null, Instant.now().plusSeconds(3600), null
        );
        connectionRepository.save(conn);

        mockMvc.perform(delete("/api/v1/integrations/github/disconnect")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        assertThat(connectionRepository.findByUserId(alice.getId())).isEmpty();

        // Subsequent status check returns disconnected
        mockMvc.perform(get("/api/v1/integrations/github/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected", is(false)));
    }

    // =========================================================================
    // 4. REPOSITORY SYNCHRONIZATION & MANAGEMENT
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/integrations/github/sync - Imports and updates repositories idempotently")
    void syncRepositories_Success() throws Exception {
        GitHubConnection conn = new GitHubConnection(
                alice, 12345L, "alice_syncer", "https://avatar", "https://profile",
                encryptionService.encrypt("sync_token"), null, Instant.now().plusSeconds(3600), null
        );
        conn = connectionRepository.save(conn);

        GitHubRepoDto repo1 = new GitHubRepoDto(
                101L, "repo-public", "alice_syncer/repo-public", "Public repo", "https://github.com/alice/repo-public",
                false, "public", "main", "Java", 42, 5, 1, Instant.now(), Instant.now(), Instant.now(),
                new GitHubRepoDto.GitHubRepoOwnerDto("alice_syncer", "https://avatar")
        );

        GitHubRepoDto repo2 = new GitHubRepoDto(
                102L, "repo-private", "alice_syncer/repo-private", "Private repo", "https://github.com/alice/repo-private",
                true, "private", "main", "TypeScript", 10, 2, 0, Instant.now(), Instant.now(), Instant.now(),
                new GitHubRepoDto.GitHubRepoOwnerDto("alice_syncer", "https://avatar")
        );

        stubClient().repositories = List.of(repo1, repo2);

        // First Sync
        mockMvc.perform(post("/api/v1/integrations/github/sync")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount", is(2)))
                .andExpect(jsonPath("$.totalCount", is(2)));

        List<GitHubRepository> savedRepos = gitHubRepositoryRepository.findByConnectionId(conn.getId());
        assertThat(savedRepos).hasSize(2);

        // Second Sync - Idempotent update without duplication
        mockMvc.perform(post("/api/v1/integrations/github/sync")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount", is(0)))
                .andExpect(jsonPath("$.updatedCount", is(2)))
                .andExpect(jsonPath("$.totalCount", is(2)));

        assertThat(gitHubRepositoryRepository.findByConnectionId(conn.getId())).hasSize(2);
    }

    @Test
    @DisplayName("PATCH showcase - Public repo showcase works, Private repo showcase rejected")
    void showcase_PrivacyEnforced() throws Exception {
        GitHubConnection conn = new GitHubConnection(
                alice, 12345L, "alice_showcase", "https://avatar", "https://profile",
                encryptionService.encrypt("token"), null, Instant.now().plusSeconds(3600), null
        );
        conn = connectionRepository.save(conn);

        GitHubRepository publicRepo = new GitHubRepository(
                conn, 201L, "alice_showcase", "public-showcase-repo", "alice_showcase/public-showcase-repo",
                "Description", "https://github.com/alice/public-showcase-repo", RepositoryVisibility.PUBLIC,
                false, "main", "Java", 100, 20, 3, Instant.now(), Instant.now(), Instant.now()
        );
        publicRepo = gitHubRepositoryRepository.save(publicRepo);

        GitHubRepository privateRepo = new GitHubRepository(
                conn, 202L, "alice_showcase", "private-repo", "alice_showcase/private-repo",
                "Description", "https://github.com/alice/private-repo", RepositoryVisibility.PRIVATE,
                true, "main", "Rust", 5, 0, 0, Instant.now(), Instant.now(), Instant.now()
        );
        privateRepo = gitHubRepositoryRepository.save(privateRepo);

        // 1. Showcase Public Repo - SUCСESS
        mockMvc.perform(patch("/api/v1/integrations/github/repositories/" + publicRepo.getId() + "/showcase")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRepoShowcaseRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isShowcased", is(true)));

        // 2. Showcase Private Repo - REJECTED (Privacy enforcement)
        mockMvc.perform(patch("/api/v1/integrations/github/repositories/" + privateRepo.getId() + "/showcase")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateRepoShowcaseRequest(true))))
                .andExpect(status().isBadRequest());

        // 3. Public showcase endpoint returns only public showcased repo
        mockMvc.perform(get("/api/v1/integrations/github/showcase/" + alice.getUsername()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("public-showcase-repo")))
                .andExpect(jsonPath("$[0].isPrivate", is(false)));
    }

    // =========================================================================
    // 5. PROJECT GITHUB REPOSITORY LINKING
    // =========================================================================

    @Test
    @DisplayName("Project Repo Linking - Owner can link and unlink; non-owner forbidden")
    void projectRepoLinking_OwnerOnlyAndPrivacy() throws Exception {
        GitHubConnection conn = new GitHubConnection(
                alice, 12345L, "alice_proj_link", "https://avatar", "https://profile",
                encryptionService.encrypt("token"), null, Instant.now().plusSeconds(3600), null
        );
        conn = connectionRepository.save(conn);

        GitHubRepository repo = new GitHubRepository(
                conn, 301L, "alice_proj_link", "cool-project-repo", "alice_proj_link/cool-project-repo",
                "Cool repo description", "https://github.com/alice/cool-project-repo", RepositoryVisibility.PUBLIC,
                false, "main", "Kotlin", 50, 10, 1, Instant.now(), Instant.now(), Instant.now()
        );
        repo = gitHubRepositoryRepository.save(repo);

        // 1. Bob (non-owner) attempts to link repository to Alice's project -> 403 Forbidden
        mockMvc.perform(post("/api/v1/projects/" + aliceProject.getId() + "/github-repositories")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkProjectRepositoryRequest(repo.getId()))))
                .andExpect(status().isForbidden());

        // 2. Alice (owner) links repository -> 201 Created
        mockMvc.perform(post("/api/v1/projects/" + aliceProject.getId() + "/github-repositories")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkProjectRepositoryRequest(repo.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.repository.name", is("cool-project-repo")));

        // 3. Duplicate linking rejected
        mockMvc.perform(post("/api/v1/projects/" + aliceProject.getId() + "/github-repositories")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkProjectRepositoryRequest(repo.getId()))))
                .andExpect(status().isBadRequest());

        // 4. List linked repositories
        mockMvc.perform(get("/api/v1/projects/" + aliceProject.getId() + "/github-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].repository.name", is("cool-project-repo")));

        // 5. Alice unlinks repository -> 204 No Content
        mockMvc.perform(delete("/api/v1/projects/" + aliceProject.getId() + "/github-repositories/" + repo.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        // List is now empty
        mockMvc.perform(get("/api/v1/projects/" + aliceProject.getId() + "/github-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Project Repo Linking - Private linked repo is hidden from public non-members")
    void projectRepoLinking_PrivateRepoHiddenFromPublic() throws Exception {
        GitHubConnection conn = new GitHubConnection(
                alice, 12345L, "alice_priv", "https://avatar", "https://profile",
                encryptionService.encrypt("token"), null, Instant.now().plusSeconds(3600), null
        );
        conn = connectionRepository.save(conn);

        GitHubRepository privateRepo = new GitHubRepository(
                conn, 401L, "alice_priv", "confidential-code", "alice_priv/confidential-code",
                "Internal proprietary core", "https://github.com/alice/confidential-code", RepositoryVisibility.PRIVATE,
                true, "main", "Go", 1, 0, 0, Instant.now(), Instant.now(), Instant.now()
        );
        privateRepo = gitHubRepositoryRepository.save(privateRepo);

        // Alice links private repo
        mockMvc.perform(post("/api/v1/projects/" + aliceProject.getId() + "/github-repositories")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LinkProjectRepositoryRequest(privateRepo.getId()))))
                .andExpect(status().isCreated());

        // Alice (owner) sees the linked private repo
        mockMvc.perform(get("/api/v1/projects/" + aliceProject.getId() + "/github-repositories")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].repository.name", is("confidential-code")));

        // Bob (non-member) does NOT see the private repo (redacted for privacy)
        mockMvc.perform(get("/api/v1/projects/" + aliceProject.getId() + "/github-repositories")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // Unauthenticated public visitor also does NOT see private repo
        mockMvc.perform(get("/api/v1/projects/" + aliceProject.getId() + "/github-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

