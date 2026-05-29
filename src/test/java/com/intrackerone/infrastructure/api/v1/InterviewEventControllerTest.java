package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.interview.EventFormat;
import com.intrackerone.domain.interview.InterviewEventDto;
import com.intrackerone.domain.interview.InterviewEventForm;
import com.intrackerone.domain.interview.InterviewEventService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InterviewEventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InterviewEventService interviewEventService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private InterviewEventController interviewEventController;

    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private InterviewEventDto eventDto;
    private InterviewEventForm form;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(interviewEventController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        eventDto = new InterviewEventDto(1L, 1L, "Phone Screen",
            LocalDateTime.of(2025, 6, 1, 10, 0), 45, EventFormat.VIDEO,
            "Jane", "Good", LocalDateTime.now());

        form = new InterviewEventForm();
        form.setTitle("Phone Screen");
        form.setEventDate(LocalDateTime.of(2025, 6, 1, 10, 0));
        form.setDurationMinutes(45);
        form.setFormat(EventFormat.VIDEO);
    }

    @Test
    void findByJob_returnsEvents() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(interviewEventService.findByJobId(1L, 1L, Role.ADMIN)).thenReturn(List.of(eventDto));

        mockMvc.perform(get("/api/v1/jobs/1/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(interviewEventService.create(eq(1L), any(InterviewEventForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(eventDto);

        mockMvc.perform(post("/api/v1/jobs/1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Phone Screen"));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(interviewEventService.update(eq(1L), any(InterviewEventForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(eventDto);

        mockMvc.perform(put("/api/v1/events/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(delete("/api/v1/events/1"))
            .andExpect(status().isNoContent());
    }
}
