package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobForm;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.job.JobType;
import com.intrackerone.domain.job.JobTimezone;
import com.intrackerone.domain.job.OnCallSchedule;
import com.intrackerone.domain.job.Source;
import com.intrackerone.domain.job.WorkType;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JobService jobService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private JobController jobController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User user;
    private JobDto jobDto;
    private JobForm form;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(jobController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        jobDto = new JobDto(
            1L, "Java Developer", null, null, null, null,   // id..salaryRange
            null, null, null, null, null, null, null, null,  // jobType..repostedAt
            null, null, null,                                // source, sourceMeta, notes
            1L, "ProjectX", null, null,                      // projectId..companyName
            1L, "Jane Doe",                                  // recruiterId, recruiterName
            1L, "Applied", "#2196F3",                        // status id/name/color
            null, null                                       // createdAt, updatedAt
        );

        form = new JobForm();
        form.setPosition("Java Developer");
        form.setJobType(JobType.FULL_TIME);
        form.setWorkType(WorkType.HYBRID);
        form.setOnCallSchedule(OnCallSchedule.NONE);
        form.setTimezone(JobTimezone.CET);
        form.setSource(Source.LINKEDIN);
        form.setProjectId(1L);
        form.setCurrentStatusId(1L);
    }

    @Test
    void findAll_returnsJobs() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(jobService.findAll(1L, Role.ADMIN, null, null, null)).thenReturn(List.of(jobDto));

        mockMvc.perform(get("/api/v1/jobs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_returnsJob() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(jobService.findById(1L, 1L, Role.ADMIN)).thenReturn(jobDto);

        mockMvc.perform(get("/api/v1/jobs/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.position").value("Java Developer"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(jobService.create(any(JobForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(jobDto);

        mockMvc.perform(post("/api/v1/jobs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.position").value("Java Developer"));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(jobService.update(eq(1L), any(JobForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(jobDto);

        mockMvc.perform(put("/api/v1/jobs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(delete("/api/v1/jobs/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void changeStatus_returnsOk() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(patch("/api/v1/jobs/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"statusId\":2,\"notes\":\"Moving forward\",\"decidedBy\":\"COMPANY\"}"))
            .andExpect(status().isOk());
    }
}
