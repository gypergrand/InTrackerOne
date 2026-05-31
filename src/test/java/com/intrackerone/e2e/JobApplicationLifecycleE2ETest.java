package com.intrackerone.e2e;

import com.intrackerone.domain.company.Company;
import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyForm;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.job.JobApplication;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobForm;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.job.JobType;
import com.intrackerone.domain.job.Source;
import com.intrackerone.domain.project.Project;
import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectForm;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.status.PipelineStatus;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.status.StatusType;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end workflow test: Complete job application lifecycle
 * - Create company and project
 * - Create and track job applications
 * - Move through pipeline stages
 * - Verify status history
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
@Slf4j
@WithMockUser(username = "admin", roles = "ADMIN")
class JobApplicationLifecycleE2ETest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private JobService jobService;

    @Autowired
    private PipelineStatusRepository pipelineStatusRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    private User testUser;
    private Company company;
    private Project project;
    private List<PipelineStatus> statuses;
    private final Role userRole = Role.USER;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByUsername("admin").orElseThrow();

        CompanyForm companyForm = new CompanyForm();
        companyForm.setName("TechCorp Inc.");
        companyForm.setWebsite("https://techcorp.com");
        companyForm.setNotes("A leading tech company");
        CompanyDto companyDto = companyService.create(companyForm, testUser.getId());
        company = Company.builder().id(companyDto.id()).name(companyDto.name()).owner(testUser).build();

        ProjectForm projectForm = new ProjectForm();
        projectForm.setCompanyId(companyDto.id());
        projectForm.setName("Backend Platform");
        projectForm.setDescription("Building scalable backend services");
        ProjectDto projectDto = projectService.create(projectForm, testUser.getId(), userRole);
        project = Project.builder().id(projectDto.id()).name(projectDto.name()).company(company).owner(testUser).build();

        statuses = pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(testUser.getId());
        statuses.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

        log.info("Setup: company={}, project={}, {} statuses", company.getId(), project.getId(), statuses.size());
    }

    @Test
    void completeJobApplicationLifecycle_fromApplicationToOffer() {
        // ========== STEP 1: Create job application ==========
        JobForm jobForm = new JobForm();
        jobForm.setProjectId(project.getId());
        jobForm.setPosition("Senior Software Engineer");
        jobForm.setJobDescription("Building distributed systems with Java and Kubernetes");
        jobForm.setJobUrl("https://techcorp.com/jobs/123");
        jobForm.setLocation("Remote, US");
        jobForm.setSalaryRange("150k - 180k");
        jobForm.setJobType(JobType.FULL_TIME);
        jobForm.setSource(Source.LINKEDIN);
        jobForm.setSourceMeta("https://linkedin.com/jobs/123");
        jobForm.setNotes("Great company, interesting tech stack");
        jobForm.setCurrentStatusId(statuses.get(1).getId()); // Applied status

        JobDto jobDto = jobService.create(jobForm, testUser.getId(), userRole);
        assertNotNull(jobDto.id());
        assertEquals("Senior Software Engineer", jobDto.position());
        log.info("Job application created with ID {}: {}", jobDto.id(), jobDto.position());

        // ========== STEP 2: Move to phone screen ==========
        Long jobId = jobDto.id();
        jobForm.setCurrentStatusId(statuses.get(2).getId()); // Phone Screen
        JobDto phoneScreenJob = jobService.update(jobId, jobForm, testUser.getId(), userRole);
        assertEquals(statuses.get(2).getId(), phoneScreenJob.currentStatusId());
        log.info("Moved to phone screen status");

        // ========== STEP 3: Move to technical interview ==========
        jobForm.setCurrentStatusId(statuses.get(3).getId()); // Technical Interview
        JobDto technicalJob = jobService.update(jobId, jobForm, testUser.getId(), userRole);
        assertEquals(statuses.get(3).getId(), technicalJob.currentStatusId());
        log.info("Moved to technical interview status");

        // ========== STEP 4: Move to on-site ==========
        jobForm.setCurrentStatusId(statuses.get(4).getId()); // On-site / Final
        JobDto onSiteJob = jobService.update(jobId, jobForm, testUser.getId(), userRole);
        assertEquals(statuses.get(4).getId(), onSiteJob.currentStatusId());
        log.info("Moved to on-site interview status");

        // ========== STEP 5: Move to offer ==========
        jobForm.setCurrentStatusId(statuses.get(5).getId()); // Offer
        JobDto offerJob = jobService.update(jobId, jobForm, testUser.getId(), userRole);
        assertEquals(statuses.get(5).getId(), offerJob.currentStatusId());
        log.info("Moved to offer status");

        // ========== STEP 6: Accept offer ==========
        jobForm.setCurrentStatusId(statuses.get(7).getId()); // Accepted (terminal)
        JobDto acceptedJob = jobService.update(jobId, jobForm, testUser.getId(), userRole);
        assertEquals(statuses.get(7).getId(), acceptedJob.currentStatusId());
        log.info("Offer accepted!");

        // ========== VERIFICATION ==========
        JobApplication finalJob = jobApplicationRepository.findById(jobId).orElseThrow();
        assertEquals(statuses.get(7).getId(), finalJob.getCurrentStatus().getId());
        assertEquals(StatusType.TERMINAL, finalJob.getCurrentStatus().getType());
        log.info("Job is in terminal accepted status");
    }

    @Test
    void jobApplicationRejection_userWithdraw() {
        // Create application
        JobForm jobForm = new JobForm();
        jobForm.setProjectId(project.getId());
        jobForm.setPosition("Junior Developer");
        jobForm.setJobType(JobType.FULL_TIME);
        jobForm.setSource(Source.LINKEDIN);
        jobForm.setCurrentStatusId(statuses.get(1).getId());
        JobDto job = jobService.create(jobForm, testUser.getId(), userRole);

        // Withdraw application
        PipelineStatus withdrawnStatus = statuses.stream()
            .filter(s -> "Withdrawn".equals(s.getName()))
            .findFirst()
            .orElseThrow();

        jobForm.setCurrentStatusId(withdrawnStatus.getId());
        JobDto withdrawn = jobService.update(job.id(), jobForm, testUser.getId(), userRole);
        assertEquals(withdrawnStatus.getId(), withdrawn.currentStatusId());
        log.info("Application withdrawn");
    }

    @Test
    void multipleApplicationsInDifferentStages() {
        // Create 3 applications
        JobForm form1 = createJobForm(project.getId(), "Frontend Dev", statuses.get(1).getId());
        JobForm form2 = createJobForm(project.getId(), "Backend Dev", statuses.get(1).getId());
        JobForm form3 = createJobForm(project.getId(), "DevOps Engineer", statuses.get(1).getId());

        JobDto job1 = jobService.create(form1, testUser.getId(), userRole);
        JobDto job2 = jobService.create(form2, testUser.getId(), userRole);
        JobDto job3 = jobService.create(form3, testUser.getId(), userRole);

        // Move to different stages
        form1.setCurrentStatusId(statuses.get(2).getId()); // Phone screen
        jobService.update(job1.id(), form1, testUser.getId(), userRole);

        form2.setCurrentStatusId(statuses.get(3).getId()); // Technical
        jobService.update(job2.id(), form2, testUser.getId(), userRole);

        form3.setCurrentStatusId(statuses.get(5).getId()); // Offer
        jobService.update(job3.id(), form3, testUser.getId(), userRole);

        // Verify applications in different statuses (filter by positions created in this test to avoid
        // interference from other @BeforeEach-created jobs in the same @Transactional test class)
        List<JobDto> allJobs = jobService.findAll(testUser.getId(), userRole, null, null, null);
        List<JobDto> thisTestsJobs = allJobs.stream()
            .filter(j -> List.of("Frontend Dev", "Backend Dev", "DevOps Engineer").contains(j.position()))
            .toList();
        assertEquals(3, thisTestsJobs.size());

        long phoneScreenCount = thisTestsJobs.stream()
            .filter(j -> j.currentStatusId().equals(statuses.get(2).getId()))
            .count();
        assertEquals(1, phoneScreenCount);

        long technicalCount = thisTestsJobs.stream()
            .filter(j -> j.currentStatusId().equals(statuses.get(3).getId()))
            .count();
        assertEquals(1, technicalCount);

        long offerCount = thisTestsJobs.stream()
            .filter(j -> j.currentStatusId().equals(statuses.get(5).getId()))
            .count();
        assertEquals(1, offerCount);

        log.info("Multiple applications tracked at different stages");
    }

    @Test
    void jobApplicationFilteringByCompanyAndProject() {
        // Create second company and project
        CompanyForm companyForm2 = new CompanyForm();
        companyForm2.setName("Another Corp");
        companyForm2.setWebsite("https://another.com");
        CompanyDto company2 = companyService.create(companyForm2, testUser.getId());

        ProjectForm projectForm2 = new ProjectForm();
        projectForm2.setCompanyId(company2.id());
        projectForm2.setName("Frontend");
        projectForm2.setDescription("");
        ProjectDto project2 = projectService.create(projectForm2, testUser.getId(), userRole);

        // Create jobs in both projects
        JobForm form1 = createJobForm(project.getId(), "Job in Project1", statuses.get(1).getId());
        JobForm form2 = createJobForm(project.getId(), "Another Job in Project1", statuses.get(1).getId());
        JobForm form3 = createJobForm(project2.id(), "Job in Project2", statuses.get(1).getId());

        jobService.create(form1, testUser.getId(), userRole);
        jobService.create(form2, testUser.getId(), userRole);
        jobService.create(form3, testUser.getId(), userRole);

        // Filter by project
        List<JobDto> project1Jobs = jobService.findAll(testUser.getId(), userRole, null, null, project.getId());
        assertEquals(2, project1Jobs.size());

        List<JobDto> project2Jobs = jobService.findAll(testUser.getId(), userRole, null, null, project2.id());
        assertEquals(1, project2Jobs.size());

        log.info("Jobs filtered correctly by project");
    }

    @Test
    void jobApplicationFiltering_ByStatus() {
        // Create jobs in different statuses
        JobForm form = createJobForm(project.getId(), "Test Job", statuses.get(1).getId());
        JobDto job1 = jobService.create(form, testUser.getId(), userRole);

        JobForm form2 = createJobForm(project.getId(), "Test Job 2", statuses.get(1).getId());
        JobDto job2 = jobService.create(form2, testUser.getId(), userRole);

        // Move job2 to different status
        form2.setCurrentStatusId(statuses.get(3).getId());
        jobService.update(job2.id(), form2, testUser.getId(), userRole);

        // Filter by status AND the project created in setUp() to avoid counting
        // jobs from other test methods (since the whole class is @Transactional)
        List<JobDto> appliedJobs = jobService.findAll(testUser.getId(), userRole,
                statuses.get(1).getId(), null, project.getId());
        assertEquals(1, appliedJobs.size());

        List<JobDto> technicalJobs = jobService.findAll(testUser.getId(), userRole,
                statuses.get(3).getId(), null, project.getId());
        assertEquals(1, technicalJobs.size());

        log.info("Jobs filtered correctly by status");
    }

    private JobForm createJobForm(Long projectId, String position, Long statusId) {
        JobForm form = new JobForm();
        form.setProjectId(projectId);
        form.setPosition(position);
        form.setJobDescription("Description");
        form.setJobUrl("https://example.com");
        form.setLocation("Remote");
        form.setSalaryRange("80k - 120k");
        form.setJobType(JobType.FULL_TIME);
        form.setSource(Source.LINKEDIN);
        form.setSourceMeta("https://linkedin.com");
        form.setNotes("Notes");
        form.setCurrentStatusId(statusId);
        return form;
    }
}