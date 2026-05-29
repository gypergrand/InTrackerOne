package com.intrackerone.infrastructure.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserDto;
import com.intrackerone.domain.user.UserForm;
import com.intrackerone.domain.user.UserService;
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
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private SecurityService securityService;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private User adminUser;
    private UserDto userDto;
    private UserForm form;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        adminUser = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        userDto = new UserDto(1L, "admin", "admin@test.com", true, Role.ADMIN);

        form = new UserForm();
        form.setUsername("admin");
        form.setPassword("pass");
        form.setEmail("admin@test.com");
        form.setRole(Role.ADMIN);
    }

    @Test
    void findAll_returnsUsers() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(userService.findAll()).thenReturn(List.of(userDto));

        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void findById_returnsUser() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(userService.findById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(userService.create(any(UserForm.class))).thenReturn(userDto);

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isCreated());
    }

    @Test
    void update_returnsUpdated() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);
        when(userService.update(eq(1L), any(UserForm.class))).thenReturn(userDto);

        mockMvc.perform(put("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(adminUser);

        mockMvc.perform(delete("/api/v1/users/1"))
            .andExpect(status().isNoContent());
    }
}
