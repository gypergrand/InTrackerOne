package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.recruiter.RecruiterDto;
import com.intrackerone.domain.recruiter.RecruiterForm;
import com.intrackerone.domain.recruiter.RecruiterService;
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
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecruiterControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecruiterService recruiterService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private RecruiterController recruiterController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User user;
    private RecruiterDto recruiterDto;
    private RecruiterForm form;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(recruiterController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        recruiterDto = new RecruiterDto(1L, "Jane Doe", "jane@test.com", "+123",
            "Good", 1L, "TestCorp", Set.of(1L));

        form = new RecruiterForm();
        form.setName("Jane Doe");
        form.setEmail("jane@test.com");
        form.setCompanyId(1L);
    }

    @Test
    void findByCompany_returnsRecruiters() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(recruiterService.findByCompanyId(1L, 1L, Role.ADMIN)).thenReturn(List.of(recruiterDto));

        mockMvc.perform(get("/api/v1/recruiters").param("companyId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_returnsRecruiter() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(recruiterService.findById(1L, 1L, Role.ADMIN)).thenReturn(recruiterDto);

        mockMvc.perform(get("/api/v1/recruiters/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(recruiterService.create(any(RecruiterForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(recruiterDto);

        mockMvc.perform(post("/api/v1/recruiters")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Jane Doe"));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(recruiterService.update(eq(1L), any(RecruiterForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(recruiterDto);

        mockMvc.perform(put("/api/v1/recruiters/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(delete("/api/v1/recruiters/1"))
            .andExpect(status().isNoContent());
    }
}
