package com.intrackerone.domain.user;

import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PipelineStatusService pipelineStatusService;

    @Mock
    private ApiTokenRepository apiTokenRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RecruiterRepository recruiterRepository;

    @Mock
    private PipelineStatusRepository pipelineStatusRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User adminUser;
    private User regularUser;
    private UserDto adminDto;
    private UserDto regularDto;
    private UserForm form;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
            .id(1L).username("admin").password("hashed")
            .email("admin@test.com").enabled(true).role(Role.ADMIN)
            .build();

        regularUser = User.builder()
            .id(2L).username("regular").password("hashed")
            .email("regular@test.com").enabled(true).role(Role.USER)
            .build();

        adminDto = new UserDto(1L, "admin", "admin@test.com", true, Role.ADMIN);
        regularDto = new UserDto(2L, "regular", "regular@test.com", true, Role.USER);

        form = new UserForm();
        form.setUsername("newuser");
        form.setPassword("rawpass");
        form.setEmail("user@test.com");
        form.setRole(Role.USER);
    }

    @Test
    void findAll_returnsAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(adminUser));
        when(userMapper.toDto(adminUser)).thenReturn(adminDto);

        List<UserDto> result = userService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).username()).isEqualTo("admin");
    }

    @Test
    void findById_whenExists_returnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userMapper.toDto(adminUser)).thenReturn(adminDto);

        UserDto result = userService.findById(1L);

        assertThat(result.username()).isEqualTo("admin");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_encodesPasswordAndSaves() {
        when(passwordEncoder.encode("rawpass")).thenReturn("encoded");
        when(userMapper.toEntity(form)).thenReturn(User.builder().username("newuser").email("user@test.com").build());
        when(userRepository.save(any())).thenReturn(adminUser);
        when(userMapper.toDto(adminUser)).thenReturn(adminDto);

        UserDto result = userService.create(form);

        assertThat(result).isNotNull();
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded");
    }

    @Test
    void update_changesEnabledField() {
        UserForm updateForm = new UserForm();
        updateForm.setUsername("regular");
        updateForm.setEmail("regular@test.com");
        updateForm.setRole(Role.USER);
        updateForm.setEnabled(false);

        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(userRepository.save(any())).thenReturn(regularUser);
        when(userMapper.toDto(regularUser)).thenReturn(regularDto);

        userService.update(2L, updateForm);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEnabled()).isFalse();
    }

    @Test
    void update_adminUserDisable_throwsException() {
        UserForm updateForm = new UserForm();
        updateForm.setUsername("admin");
        updateForm.setEmail("admin@test.com");
        updateForm.setRole(Role.ADMIN);
        updateForm.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.update(1L, updateForm))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Cannot disable the admin user");
    }

    @Test
    void delete_whenExists_deletes() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(apiTokenRepository.findByUserId(2L)).thenReturn(List.of());
        when(companyRepository.findByOwnerId(2L)).thenReturn(List.of());
        when(projectRepository.findByOwnerId(2L)).thenReturn(List.of());
        when(recruiterRepository.findByOwnerId(2L)).thenReturn(List.of());
        when(pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(2L)).thenReturn(List.of());
        when(jobApplicationRepository.findByOwnerId(2L)).thenReturn(List.of());

        userService.delete(2L);

        verify(userRepository).deleteById(2L);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_adminUser_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.delete(1L))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Cannot delete the admin user");
    }

    @Test
    void changePassword_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("oldPass", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHashed");

        userService.changePassword(1L, "oldPass", "newPass");

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("newHashed");
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, "wrong", "newPass"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void changePassword_blankPasswords_throwsException() {
        assertThatThrownBy(() -> userService.changePassword(1L, "", "newPass"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.changePassword(1L, "old", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
