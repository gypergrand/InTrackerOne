package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.compensation.CompCategory;
import com.intrackerone.domain.compensation.CompensationEntryDto;
import com.intrackerone.domain.compensation.CompensationEntryForm;
import com.intrackerone.domain.compensation.CompensationEntryService;
import com.intrackerone.domain.compensation.Currency;
import com.intrackerone.domain.compensation.RateType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CompensationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CompensationEntryService compensationEntryService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private CompensationController compensationController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private CompensationEntryDto entryDto;
    private CompensationEntryForm form;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(compensationController).build();
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        entryDto = new CompensationEntryDto(1L, 1L, RateType.ANNUAL_GROSS,
            new BigDecimal("100000"), Currency.EUR, CompCategory.OFFER,
            "Great offer", LocalDateTime.now());

        form = new CompensationEntryForm();
        form.setRateType(RateType.ANNUAL_GROSS);
        form.setAmount(new BigDecimal("100000"));
        form.setCurrency(Currency.EUR);
        form.setCategory(CompCategory.OFFER);
    }

    @Test
    void findByJob_returnsEntries() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(compensationEntryService.findByJobId(1L, 1L, Role.ADMIN)).thenReturn(List.of(entryDto));

        mockMvc.perform(get("/api/v1/jobs/1/compensation"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(compensationEntryService.create(eq(1L), any(CompensationEntryForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(entryDto);

        mockMvc.perform(post("/api/v1/jobs/1/compensation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(100000));
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        when(compensationEntryService.update(eq(1L), any(CompensationEntryForm.class), eq(1L), eq(Role.ADMIN))).thenReturn(entryDto);

        mockMvc.perform(put("/api/v1/compensation/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(user);
        mockMvc.perform(delete("/api/v1/compensation/1"))
            .andExpect(status().isNoContent());
    }
}
