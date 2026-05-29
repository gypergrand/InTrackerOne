package com.intrackerone.domain.recruiter;

import com.intrackerone.domain.company.Company;
import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.project.Project;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecruiterServiceTest {

    @Mock
    private RecruiterRepository recruiterRepository;

    @Mock
    private RecruiterMapper recruiterMapper;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecruiterService recruiterService;

    @Captor
    private ArgumentCaptor<Recruiter> recruiterCaptor;

    private User admin;
    private User normalUser;
    private Company company;
    private Project project;
    private Recruiter recruiter;
    private RecruiterDto recruiterDto;
    private RecruiterForm form;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        normalUser = User.builder().id(2L).username("user").role(Role.USER).build();
        company = Company.builder().id(1L).name("TestCorp").owner(admin).build();
        project = Project.builder().id(1L).name("Platform").company(company).owner(admin).build();

        recruiter = Recruiter.builder()
            .id(1L).name("Jane Doe").email("jane@test.com").phone("+123")
            .notes("Good").company(company).owner(admin).projects(Set.of(project))
            .build();

        recruiterDto = new RecruiterDto(1L, "Jane Doe", "jane@test.com", "+123",
            "Good", 1L, "TestCorp", Set.of(1L));

        form = new RecruiterForm();
        form.setName("Jane Doe");
        form.setEmail("jane@test.com");
        form.setPhone("+123");
        form.setNotes("Good");
        form.setCompanyId(1L);
        form.setProjectIds(Set.of(1L));
    }

    @Test
    void findByCompanyId_forAdmin_returnsAll() {
        when(recruiterRepository.findByCompanyId(1L)).thenReturn(List.of(recruiter));
        when(recruiterMapper.toDto(recruiter)).thenReturn(recruiterDto);

        List<RecruiterDto> result = recruiterService.findByCompanyId(1L, 1L, Role.ADMIN);

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCompanyId_forUser_filtersOwn() {
        when(recruiterRepository.findByCompanyId(1L)).thenReturn(List.of(recruiter));

        List<RecruiterDto> result = recruiterService.findByCompanyId(1L, 2L, Role.USER);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_whenExists_returnsRecruiter() {
        when(recruiterRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(recruiterMapper.toDto(recruiter)).thenReturn(recruiterDto);

        RecruiterDto result = recruiterService.findById(1L, 1L, Role.ADMIN);

        assertThat(result.name()).isEqualTo("Jane Doe");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        when(recruiterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruiterService.findById(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_savesWithCompanyOwnerAndProjects() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(recruiterMapper.toEntity(form)).thenReturn(recruiter);
        when(recruiterRepository.save(any())).thenReturn(recruiter);
        when(recruiterMapper.toDto(recruiter)).thenReturn(recruiterDto);

        RecruiterDto result = recruiterService.create(form, 1L, Role.ADMIN);

        assertThat(result.name()).isEqualTo("Jane Doe");
        verify(recruiterRepository).save(recruiterCaptor.capture());
        Recruiter saved = recruiterCaptor.getValue();
        assertThat(saved.getCompany()).isEqualTo(company);
        assertThat(saved.getOwner()).isEqualTo(admin);
        assertThat(saved.getProjects()).contains(project);
    }

    @Test
    void update_modifiesFieldsAndProjects() {
        RecruiterForm updateForm = new RecruiterForm();
        updateForm.setName("Jane Smith");
        updateForm.setEmail("jane@new.com");
        updateForm.setPhone("+456");
        updateForm.setNotes("Updated");
        updateForm.setProjectIds(Set.of(1L));

        when(recruiterRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(recruiterRepository.save(any())).thenReturn(recruiter);
        when(recruiterMapper.toDto(recruiter)).thenReturn(recruiterDto);

        recruiterService.update(1L, updateForm, 1L, Role.ADMIN);

        verify(recruiterRepository).save(recruiterCaptor.capture());
        Recruiter saved = recruiterCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Jane Smith");
        assertThat(saved.getEmail()).isEqualTo("jane@new.com");
        assertThat(saved.getPhone()).isEqualTo("+456");
        assertThat(saved.getNotes()).isEqualTo("Updated");
    }

    @Test
    void delete_whenExists_deletes() {
        when(recruiterRepository.findById(1L)).thenReturn(Optional.of(recruiter));

        recruiterService.delete(1L, 1L, Role.ADMIN);

        verify(recruiterRepository).delete(recruiter);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(recruiterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruiterService.delete(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_whenCompanyNotFound_throwsException() {
        form.setCompanyId(99L);
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruiterService.create(form, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_whenUserDoesNotOwnCompany_throwsException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> recruiterService.create(form, 2L, Role.USER))
            .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void create_whenProjectNotFound_throwsException() {
        form.setProjectIds(Set.of(99L));
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(recruiterMapper.toEntity(form)).thenReturn(recruiter);
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recruiterService.create(form, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
