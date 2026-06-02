package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyForm;
import com.intrackerone.domain.company.CompanyService;
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
class CompanyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CompanyService companyService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private CompanyController companyController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private User adminUser;
    private CompanyDto companyDto;
    private CompanyForm companyForm;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(companyController).build();

        adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        companyDto = new CompanyDto(1L, "Test Corp", "https://test.com", "notes");
        companyForm = new CompanyForm();
        companyForm.setName("Test Corp");
        companyForm.setWebsite("https://test.com");
        companyForm.setNotes("notes");
    }

    @Test
    void getAll_returnsCompanies() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(companyService.findAll(1L, Role.ADMIN)).thenReturn(List.of(companyDto));

        mockMvc.perform(get("/api/v1/companies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Test Corp"));
    }

    @Test
    void getById_returnsCompany() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(companyService.findById(1L, 1L, Role.ADMIN)).thenReturn(companyDto);

        mockMvc.perform(get("/api/v1/companies/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Corp"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(companyService.create(any(CompanyForm.class), eq(1L))).thenReturn(companyDto);

        mockMvc.perform(post("/api/v1/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(companyForm)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Corp"));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(companyService.update(eq(1L), any(CompanyForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(companyDto);

        mockMvc.perform(put("/api/v1/companies/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(companyForm)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Corp"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        mockMvc.perform(delete("/api/v1/companies/1"))
            .andExpect(status().isNoContent());
    }
}
