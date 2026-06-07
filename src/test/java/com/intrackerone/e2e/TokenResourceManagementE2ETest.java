package com.intrackerone.e2e;

import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.ApiTokenService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end test: regular USER role manages their own resources via API token.
 * Verifies the full lifecycle — token creation via API, CRUD on companies,
 * projects, recruiters, jobs via Bearer token, and that users can manage their
 * own API tokens while still being isolated from other users' tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Transactional
@Slf4j
class TokenResourceManagementE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private PipelineStatusService pipelineStatusService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User regularUser;
    private String apiToken;

    @BeforeEach
    void setUp() {
        // Ensure the test user exists (self-contained; works regardless of DataInitializer state)
        regularUser = userRepository.findByUsername("user").orElseGet(() -> {
            User user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("user"))
                .email("user@intrackerone.local")
                .enabled(true)
                .role(Role.USER)
                .build();
            user = userRepository.save(user);
            pipelineStatusService.seedDefaultsForUser(user.getId());
            return user;
        });
        ApiTokenService.CreatedToken created = apiTokenService.create(regularUser.getId(), "User API Token");
        apiToken = created.token();
        assertNotNull(apiToken);
    }

    /** Helper to extract a Long id from a JSON response. JsonPath returns Integer for small numbers. */
    private long extractId(String json) {
        return JsonPath.<Number>read(json, "$.id").longValue();
    }

    // ========== Token authority ==========

    @Test
    void user_CanUseToken_ToAccessApiEndpoints() throws Exception {
        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isOk());
    }

    @Test
    void user_CanManageOwnTokens() throws Exception {
        // List own tokens
        mockMvc.perform(
            get("/api/v1/tokens")
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isOk());

        // Create a new token via API
        String createResp = mockMvc.perform(
            post("/api/v1/tokens")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"e2e-test-token\"}")
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("e2e-test-token"))
            .andExpect(jsonPath("$.token").exists())
            .andReturn().getResponse().getContentAsString();
        long newTokenId = extractId(createResp);

        // Revoke it
        mockMvc.perform(
            delete("/api/v1/tokens/" + newTokenId)
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isNoContent());
    }

    @Test
    void user_CannotRevokeOtherUsersToken() throws Exception {
        // Create another user and a token for them, then verify the first user cannot revoke it
        User otherUser = userRepository.findByUsername("other-user").orElseGet(() -> {
            User u = User.builder()
                .username("other-user")
                .password(passwordEncoder.encode("other"))
                .email("other-user@intrackerone.local")
                .enabled(true)
                .role(Role.USER)
                .build();
            u = userRepository.save(u);
            pipelineStatusService.seedDefaultsForUser(u.getId());
            return u;
        });
        ApiTokenService.CreatedToken otherToken = apiTokenService.create(otherUser.getId(), "Other User Token");
        long otherTokenId = otherToken.id();

        mockMvc.perform(
            delete("/api/v1/tokens/" + otherTokenId)
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isForbidden());
    }

    // ========== Companies CRUD ==========

    @Test
    void user_CanCreateCompany() throws Exception {
        mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"API Corp\",\"website\":\"https://api-corp.com\",\"notes\":\"Created via API\"}")
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("API Corp"))
        .andExpect(jsonPath("$.website").value("https://api-corp.com"));
    }

    @Test
    void user_CanListOwnCompanies() throws Exception {
        mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"My Company\"}")
        ).andExpect(status().isCreated());

        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("My Company"));
    }

    @Test
    void user_CanUpdateOwnCompany() throws Exception {
        String response = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Old Name\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        long id = extractId(response);

        mockMvc.perform(
            put("/api/v1/companies/" + id)
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"New Name\",\"website\":\"https://updated.com\",\"notes\":\"updated\"}")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"));
    }

    @Test
    void user_CanDeleteOwnCompany() throws Exception {
        String response = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"To Delete\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        long id = extractId(response);

        mockMvc.perform(
            delete("/api/v1/companies/" + id)
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isNoContent());

        mockMvc.perform(
            get("/api/v1/companies/" + id)
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isNotFound());
    }

    // ========== Projects CRUD ==========

    @Test
    void user_CanCreateProject() throws Exception {
        String companyResp = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Project Parent Co\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long companyId = extractId(companyResp);

        mockMvc.perform(
            post("/api/v1/projects")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"API Project\",\"description\":\"Test project\",\"companyId\":" + companyId + ",\"industries\":[\"Software Development\"]}")
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("API Project"));
    }

    @Test
    void user_CanListProjectsByCompany() throws Exception {
        String companyResp = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"List Projects Co\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long companyId = extractId(companyResp);

        mockMvc.perform(
            post("/api/v1/projects")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Project A\",\"description\":\"First\",\"companyId\":" + companyId + ",\"industries\":[\"SaaS\"]}")
        ).andExpect(status().isCreated());

        mockMvc.perform(
            post("/api/v1/projects")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Project B\",\"description\":\"Second\",\"companyId\":" + companyId + ",\"industries\":[\"Cloud\"]}")
        ).andExpect(status().isCreated());

        mockMvc.perform(
            get("/api/v1/projects?companyId=" + companyId)
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
    }

    // ========== Recruiters CRUD ==========

    @Test
    void user_CanCreateRecruiter() throws Exception {
        String companyResp = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Recruiter Parent Co\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long companyId = extractId(companyResp);

        mockMvc.perform(
            post("/api/v1/recruiters")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Jane Recruiter\",\"email\":\"jane@example.com\",\"phone\":\"+1234567890\",\"companyId\":" + companyId + "}")
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Jane Recruiter"))
        .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void user_CanListRecruitersByCompany() throws Exception {
        String companyResp = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"List Recruiters Co\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long companyId = extractId(companyResp);

        mockMvc.perform(
            post("/api/v1/recruiters")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Bob\",\"email\":\"bob@example.com\",\"companyId\":" + companyId + "}")
        ).andExpect(status().isCreated());

        mockMvc.perform(
            get("/api/v1/recruiters?companyId=" + companyId)
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Bob"));
    }

    // ========== Jobs CRUD ==========

    @Test
    void user_CanCreateJob() throws Exception {
        String companyResp = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Job Parent Co\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long companyId = extractId(companyResp);

        String projectResp = mockMvc.perform(
            post("/api/v1/projects")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"Job Project\",\"description\":\"Test\",\"companyId\":" + companyId + ",\"industries\":[\"Engineering\"]}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long projectId = extractId(projectResp);

        String statusesResp = mockMvc.perform(
            get("/api/v1/statuses")
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long statusId = JsonPath.<Number>read(statusesResp, "$[0].id").longValue();

        mockMvc.perform(
            post("/api/v1/jobs")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"position\":\"Senior Java Developer\",\"jobType\":\"FULL_TIME\",\"workType\":\"HYBRID\","
                    + "\"onCallSchedule\":\"MONTHLY_ON_CALL\",\"onCallDetails\":\"week in a month\","
                    + "\"timezone\":\"CET\",\"source\":\"LINKEDIN\","
                    + "\"projectId\":" + projectId + ",\"currentStatusId\":" + statusId + ",\"location\":\"Remote\"}")
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.position").value("Senior Java Developer"));
    }

    @Test
    void user_CanListOwnJobs() throws Exception {
        String companyResp = mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"List Jobs Co\"}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long companyId = extractId(companyResp);

        String projectResp = mockMvc.perform(
            post("/api/v1/projects")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"List Project\",\"description\":\"Test\",\"companyId\":" + companyId + ",\"industries\":[\"Software Development\"]}")
        ).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long projectId = extractId(projectResp);

        String statusesResp = mockMvc.perform(
            get("/api/v1/statuses")
                .header("Authorization", "Bearer " + apiToken)
        ).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long statusId = JsonPath.<Number>read(statusesResp, "$[0].id").longValue();

        mockMvc.perform(
            post("/api/v1/jobs")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"position\":\"DevOps Engineer\",\"jobType\":\"CONTRACT\",\"workType\":\"REMOTE\","
                    + "\"onCallSchedule\":\"NONE\",\"timezone\":\"PST\",\"source\":\"TELEGRAM_GROUP\","
                    + "\"projectId\":" + projectId + ",\"currentStatusId\":" + statusId + "}")
        ).andExpect(status().isCreated());

        mockMvc.perform(
            get("/api/v1/jobs")
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].position").value("DevOps Engineer"));
    }

    // ========== Data isolation ==========

    @Test
    void user_DataIsolatedToOwnResources() throws Exception {
        mockMvc.perform(
            post("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
                .contentType("application/json")
                .content("{\"name\":\"User Company\"}")
        ).andExpect(status().isCreated());

        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    }
}
