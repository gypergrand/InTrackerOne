package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.history.StatusChangeDto;
import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.status.StatusType;
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
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobService jobService;

    @Mock
    private StatusChangeService statusChangeService;

    @Mock
    private PipelineStatusService pipelineStatusService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private DashboardController dashboardController;

    private User user;
    private PipelineStatusDto statusDto;
    private StatusChangeDto changeDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        statusDto = new PipelineStatusDto(1L, "Applied", 1, "#2196F3", StatusType.ACTIVE);
        changeDto = new StatusChangeDto(1L, 1L, 1L, "Applied", 2L, "Phone Screen",
            LocalDateTime.now(), "COMPANY", "Moving forward", LocalDateTime.now());
    }

    @Test
    void summary_returnsAggregatedData() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(pipelineStatusService.findByOwnerId(1L)).thenReturn(List.of(statusDto));
        when(jobService.countByOwnerIdAndStatusId(1L, 1L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalJobs").value(3))
            .andExpect(jsonPath("$.byStatus.Applied").value(3));
    }

    @Test
    void recent_returnsRecentChanges() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(statusChangeService.findRecentByOwnerId(1L, 20)).thenReturn(List.of(changeDto));

        mockMvc.perform(get("/api/v1/dashboard/recent"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }
}
