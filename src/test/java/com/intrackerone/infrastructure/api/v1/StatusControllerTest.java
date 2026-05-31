package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusForm;
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
class StatusControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PipelineStatusService pipelineStatusService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private StatusController statusController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User user;
    private PipelineStatusDto statusDto;
    private PipelineStatusForm form;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(statusController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        statusDto = new PipelineStatusDto(1L, "Applied", "#2196F3", StatusType.ACTIVE);

        form = new PipelineStatusForm();
        form.setName("Applied");
        form.setColor("#2196F3");
        form.setType(StatusType.ACTIVE);
    }

    @Test
    void findAll_returnsStatuses() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(pipelineStatusService.findByOwnerId(1L)).thenReturn(List.of(statusDto));

        mockMvc.perform(get("/api/v1/statuses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_returnsStatus() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(pipelineStatusService.findById(1L, 1L, Role.ADMIN)).thenReturn(statusDto);

        mockMvc.perform(get("/api/v1/statuses/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Applied"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(pipelineStatusService.create(any(PipelineStatusForm.class), eq(1L))).thenReturn(statusDto);

        mockMvc.perform(post("/api/v1/statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Applied"));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(pipelineStatusService.update(eq(1L), any(PipelineStatusForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(statusDto);

        mockMvc.perform(put("/api/v1/statuses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(delete("/api/v1/statuses/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void move_returnsOk() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(put("/api/v1/statuses/1/move")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newOrder\":2}"))
            .andExpect(status().isOk());
    }
}
