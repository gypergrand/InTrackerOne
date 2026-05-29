package com.intrackerone.e2e;

import com.intrackerone.domain.user.ApiToken;
import com.intrackerone.domain.user.ApiTokenRepository;
import com.intrackerone.domain.user.ApiTokenService;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end API workflow test: REST API with Bearer token authentication
 * - ADMIN user creates API token
 * - Use token to make API requests
 * - Verify authorization and access control
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
@Slf4j
class ApiAuthenticationE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiTokenService apiTokenService;

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    private User adminUser;
    private String apiToken;

    @BeforeEach
    @WithMockUser(username = "admin", roles = "ADMIN")
    void setUp() {
        adminUser = userRepository.findByUsername("admin").orElseThrow();

        // Admin creates API token for themselves
        ApiToken token = apiTokenService.createToken(adminUser.getId(), "E2E Test Token");
        apiToken = token.getTokenValue();
        assertNotNull(apiToken);
        log.info("✓ Created API token for admin user");
    }

    @Test
    void tokenCreation_AdminCanCreateToken() {
        // Verify token was created and stored
        var tokens = apiTokenRepository.findByUserId(adminUser.getId());
        assertTrue(tokens.stream().anyMatch(t -> t.getName().equals("E2E Test Token")));
        log.info("✓ Token persisted in repository");

        // Verify token is enabled
        ApiToken token = tokens.stream()
            .filter(t -> t.getName().equals("E2E Test Token"))
            .findFirst()
            .orElseThrow();
        assertTrue(token.getEnabled());
        assertNotNull(token.getCreatedAt());
        log.info("✓ Token enabled and has creation timestamp");
    }

    @Test
    void apiRequest_WithValidToken_Succeeds() throws Exception {
        // Make API request with Bearer token
        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk());

        log.info("✓ API request with valid token succeeded");
    }

    @Test
    void apiRequest_WithoutToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/companies"))
            .andExpect(status().isForbidden());

        log.info("✓ Request without token returns 403");
    }

    @Test
    void apiRequest_WithInvalidToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer invalid_token_xyz")
        )
        .andExpect(status().isForbidden());

        log.info("✓ Request with invalid token returns 403");
    }

    @Test
    void tokenRevocation_RevokedTokenBecomesForbidden() throws Exception {
        // Verify token works
        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isOk());

        log.info("✓ Token is valid and functional");

        // Revoke the token
        var tokens = apiTokenRepository.findByUserId(adminUser.getId());
        Long tokenId = tokens.stream()
            .filter(t -> t.getName().equals("E2E Test Token"))
            .findFirst()
            .orElseThrow()
            .getId();

        apiTokenService.revokeToken(adminUser.getId(), tokenId);
        log.info("✓ Token revoked");

        // Verify token no longer works
        mockMvc.perform(
            get("/api/v1/companies")
                .header("Authorization", "Bearer " + apiToken)
        )
        .andExpect(status().isForbidden());

        log.info("✓ Revoked token returns 403");
    }

    @Test
    void multipleTokens_AdminCanManageMultiple() {
        // Create second token
        ApiToken token2 = apiTokenService.createToken(adminUser.getId(), "E2E Test Token 2");
        assertNotNull(token2.getTokenValue());
        assertNotEquals(apiToken, token2.getTokenValue());

        // Verify both tokens exist
        var tokens = apiTokenRepository.findByUserId(adminUser.getId());
        assertEquals(2, tokens.stream()
            .filter(t -> t.getName().startsWith("E2E Test Token"))
            .count());

        log.info("✓ Admin can create multiple tokens");
    }
}

