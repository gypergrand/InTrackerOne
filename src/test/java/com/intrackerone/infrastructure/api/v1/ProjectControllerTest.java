package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectForm;
import com.intrackerone.domain.project.ProjectService;
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
class ProjectControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProjectService projectService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private ProjectController projectController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User user;
    private ProjectDto projectDto;
    private ProjectForm form;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        projectDto = new ProjectDto(1L, "Platform", "Backend", 1L, "TestCorp");

        form = new ProjectForm();
        form.setName("Platform");
        form.setDescription("Backend");
        form.setCompanyId(1L);
    }

    @Test
    void findByCompany_returnsProjects() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(projectService.findByCompanyId(1L, 1L, Role.ADMIN)).thenReturn(List.of(projectDto));

        mockMvc.perform(get("/api/v1/projects").param("companyId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_returnsProject() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(projectService.findById(1L, 1L, Role.ADMIN)).thenReturn(projectDto);

        mockMvc.perform(get("/api/v1/projects/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Platform"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(securityService.getCurrentUser()).thenReturn(user);
        when(projectService.create(any(ProjectForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(projectDto);

        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Platform"));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(projectService.update(eq(1L), any(ProjectForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(projectDto);

        mockMvc.perform(put("/api/v1/projects/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(delete("/api/v1/projects/1"))
            .andExpect(status().isNoContent());
    }
}
