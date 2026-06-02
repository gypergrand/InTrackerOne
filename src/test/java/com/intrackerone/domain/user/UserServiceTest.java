package com.intrackerone.domain.user;

import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
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
    private UserDto adminDto;
    private UserForm form;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
            .id(1L).username("admin").password("hashed")
            .email("admin@test.com").enabled(true).role(Role.ADMIN)
            .build();

        adminDto = new UserDto(1L, "admin", "admin@test.com", true, Role.ADMIN);

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
        updateForm.setUsername("admin");
        updateForm.setEmail("admin@test.com");
        updateForm.setRole(Role.ADMIN);
        updateForm.setEnabled(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any())).thenReturn(adminUser);
        when(userMapper.toDto(adminUser)).thenReturn(adminDto);

        userService.update(1L, updateForm);

        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEnabled()).isFalse();
    }

    @Test
    void delete_whenExists_deletes() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(apiTokenRepository.findByUserId(1L)).thenReturn(List.of());
        when(companyRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(projectRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(recruiterRepository.findByOwnerId(1L)).thenReturn(List.of());
        when(pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(1L)).thenReturn(List.of());
        when(jobApplicationRepository.findByOwnerId(1L)).thenReturn(List.of());

        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.delete(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
