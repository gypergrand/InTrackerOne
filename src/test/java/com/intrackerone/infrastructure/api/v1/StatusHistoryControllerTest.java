package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.history.StatusChangeDto;
import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StatusHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StatusChangeService statusChangeService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private StatusHistoryController statusHistoryController;

    private StatusChangeDto changeDto;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(statusHistoryController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        changeDto = new StatusChangeDto(1L, 1L, 1L, "Applied", 2L, "Phone Screen",
            LocalDateTime.now(), "COMPANY", "Moving forward", LocalDateTime.now());
    }

    @Test
    void getHistory_returnsHistory() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(statusChangeService.findByJobId(1L, 1L, Role.ADMIN)).thenReturn(List.of(changeDto));

        mockMvc.perform(get("/api/v1/jobs/1/history"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].toStatusName").value("Phone Screen"));
    }
}
