package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.user.ApiTokenService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ApiTokenService apiTokenService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private TokenController tokenController;

    private User adminUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tokenController).build();
        adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
    }

    @Test
    void findAll_returnsTokens() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(apiTokenService.findByUserId(1L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/tokens"))
            .andExpect(status().isOk());
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(apiTokenService.create(eq(1L), anyString()))
            .thenReturn(new ApiTokenService.CreatedToken(1L, "test", "itr_abc", LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/tokens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("itr_abc"));
    }

    @Test
    void revoke_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);

        mockMvc.perform(delete("/api/v1/tokens/1"))
            .andExpect(status().isNoContent());
    }
}
