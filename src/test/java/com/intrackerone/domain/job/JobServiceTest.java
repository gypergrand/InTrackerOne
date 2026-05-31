package com.intrackerone.domain.job;

import com.intrackerone.domain.company.Company;
import com.intrackerone.domain.history.StatusChangeRepository;
import com.intrackerone.domain.project.Project;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.recruiter.Recruiter;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.status.PipelineStatus;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.status.StatusType;
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
class JobServiceTest {

    @Mock
    private JobApplicationRepository jobRepository;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RecruiterRepository recruiterRepository;

    @Mock
    private PipelineStatusRepository pipelineStatusRepository;

    @Mock
    private StatusChangeRepository statusChangeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobService jobService;

    @Captor
    private ArgumentCaptor<JobApplication> jobCaptor;

    private User admin;
    private Company company;
    private Project project;
    private Recruiter recruiter;
    private PipelineStatus status;
    private JobApplication job;
    private JobDto jobDto;
    private JobForm form;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        company = Company.builder().id(1L).name("TestCorp").owner(admin).build();
        project = Project.builder().id(1L).name("ProjectX").company(company).owner(admin).build();
        recruiter = Recruiter.builder().id(1L).name("Jane Doe").company(company).owner(admin).build();
        status = PipelineStatus.builder().id(1L).name("Applied").color("#2196F3")
            .type(StatusType.ACTIVE).owner(admin).build();

        job = JobApplication.builder()
            .id(1L).position("Java Developer").jobType(JobType.FULL_TIME)
            .source(Source.LINKEDIN).project(project).recruiter(recruiter)
            .currentStatus(status).owner(admin)
            .build();

        jobDto = new JobDto(1L, "Java Developer", null, null, null, null,
            JobType.FULL_TIME, Source.LINKEDIN, null, null,
            1L, "ProjectX", null, null, 1L, "Jane Doe",
            1L, "Applied", "#2196F3", null, null);

        form = new JobForm();
        form.setPosition("Java Developer");
        form.setJobType(JobType.FULL_TIME);
        form.setSource(Source.LINKEDIN);
        form.setProjectId(1L);
        form.setCurrentStatusId(1L);
    }

    @Test
    void findAll_forAdmin_returnsAllJobs() {
        when(jobRepository.findAll()).thenReturn(List.of(job));
        when(jobMapper.toDto(job)).thenReturn(jobDto);

        List<JobDto> result = jobService.findAll(1L, Role.ADMIN, null, null, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withStatusFilter_filtersResults() {
        when(jobRepository.findAll()).thenReturn(List.of(job));

        List<JobDto> result = jobService.findAll(1L, Role.ADMIN, 99L, null, null);

        assertThat(result).isEmpty();
    }

    @Test
    void create_savesJobWithRelations() {
        JobForm createForm = new JobForm();
        createForm.setPosition("New Job");
        createForm.setJobType(JobType.CONTRACT);
        createForm.setSource(Source.COMPANY_PORTAL);
        createForm.setProjectId(1L);
        createForm.setCurrentStatusId(1L);
        createForm.setRecruiterId(1L);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(pipelineStatusRepository.findById(1L)).thenReturn(Optional.of(status));
        when(recruiterRepository.findById(1L)).thenReturn(Optional.of(recruiter));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(jobMapper.toEntity(createForm)).thenReturn(JobApplication.builder().position("New Job").build());
        when(jobRepository.save(any())).thenReturn(job);
        when(jobMapper.toDto(job)).thenReturn(jobDto);

        JobDto result = jobService.create(createForm, 1L, Role.ADMIN);

        assertThat(result).isNotNull();
        verify(jobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getProject()).isEqualTo(project);
        assertThat(jobCaptor.getValue().getCurrentStatus()).isEqualTo(status);
        assertThat(jobCaptor.getValue().getRecruiter()).isEqualTo(recruiter);
        assertThat(jobCaptor.getValue().getOwner()).isEqualTo(admin);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(jobRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.delete(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void countByOwnerAndStatusId_returnsCount() {
        when(jobRepository.countByOwnerIdAndCurrentStatusId(1L, 1L)).thenReturn(5L);

        long count = jobService.countByOwnerIdAndStatusId(1L, 1L);

        assertThat(count).isEqualTo(5L);
    }
}
