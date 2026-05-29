package com.intrackerone.domain.project;

import com.intrackerone.domain.company.Company;
import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService projectService;

    @Captor
    private ArgumentCaptor<Project> projectCaptor;

    private User admin;
    private User normalUser;
    private Company company;
    private Project project;
    private ProjectDto projectDto;
    private ProjectForm form;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        normalUser = User.builder().id(2L).username("user").role(Role.USER).build();
        company = Company.builder().id(1L).name("TestCorp").owner(admin).build();

        project = Project.builder()
            .id(1L).name("Platform").description("Backend").company(company).owner(admin)
            .build();

        projectDto = new ProjectDto(1L, "Platform", "Backend", 1L, "TestCorp");

        form = new ProjectForm();
        form.setName("Platform");
        form.setDescription("Backend");
        form.setCompanyId(1L);
    }

    @Test
    void findByCompanyId_forAdmin_returnsAll() {
        when(projectRepository.findByCompanyId(1L)).thenReturn(List.of(project));
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        List<ProjectDto> result = projectService.findByCompanyId(1L, 1L, Role.ADMIN);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCompanyId_forUser_filtersOwn() {
        when(projectRepository.findByCompanyId(1L)).thenReturn(List.of(project));

        List<ProjectDto> result = projectService.findByCompanyId(1L, 2L, Role.USER);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_whenExists_returnsProject() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        ProjectDto result = projectService.findById(1L, 1L, Role.ADMIN);

        assertThat(result.name()).isEqualTo("Platform");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.findById(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesWithCompanyAndOwner() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(projectMapper.toEntity(form)).thenReturn(project);
        when(projectRepository.save(any())).thenReturn(project);
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        ProjectDto result = projectService.create(form, 1L, Role.ADMIN);

        assertThat(result.name()).isEqualTo("Platform");
        verify(projectRepository).save(projectCaptor.capture());
        Project saved = projectCaptor.getValue();
        assertThat(saved.getCompany()).isEqualTo(company);
        assertThat(saved.getOwner()).isEqualTo(admin);
    }

    @Test
    void update_modifiesFields() {
        ProjectForm updateForm = new ProjectForm();
        updateForm.setName("New Platform");
        updateForm.setDescription("Updated backend");

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(projectRepository.save(any())).thenReturn(project);
        when(projectMapper.toDto(project)).thenReturn(projectDto);

        projectService.update(1L, updateForm, 1L, Role.ADMIN);

        verify(projectRepository).save(projectCaptor.capture());
        Project saved = projectCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("New Platform");
        assertThat(saved.getDescription()).isEqualTo("Updated backend");
    }

    @Test
    void delete_whenExists_deletes() {
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.delete(1L, 1L, Role.ADMIN);

        verify(projectRepository).delete(project);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
