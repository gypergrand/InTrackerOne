package com.intrackerone.infrastructure.data;

import com.intrackerone.domain.company.CompanyForm;
import com.intrackerone.domain.company.CompanyService;
import com.intrackerone.domain.job.JobForm;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.job.JobTimezone;
import com.intrackerone.domain.job.JobType;
import com.intrackerone.domain.job.OnCallSchedule;
import com.intrackerone.domain.job.Source;
import com.intrackerone.domain.job.WorkType;
import com.intrackerone.domain.project.ProjectForm;
import com.intrackerone.domain.project.ProjectService;
import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-off importer for the LinkedIn job posting.
 * Runs after DataInitializer (Order(2)) and idempotently creates:
 *   1. Company "Fever"
 *   2. Project "Senior/Staff Product Engineer" under that company
 *   3. Job application linked to the project with "Preparation" status
 *
 * Inactive by default. Enable only for local seeding with:
 *   SPRING_PROFILES_ACTIVE=h2,dev-seed mvn spring-boot:run
 * or in tests via @ActiveProfiles({"h2", "dev-seed"}).
 */
@Component
@Profile("dev-seed")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class LinkedInJobImporter implements CommandLineRunner {

    private static final String COMPANY_NAME = "Fever";
    private static final String JOB_TITLE = "Senior/Staff Product Engineer";
    private static final String JOB_URL = "https://www.linkedin.com/jobs/view/4400870044";

    private final CompanyService companyService;
    private final ProjectService projectService;
    private final JobService jobService;
    private final PipelineStatusService pipelineStatusService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Use the first available ADMIN or MODERATOR user, or fall back to any user
        User currentUser = userRepository.findAll().stream()
                .filter(u -> u.getEnabled() != null && u.getEnabled())
                .findFirst()
                .orElse(null);

        if (currentUser == null) {
            log.warn("LinkedInJobImporter: no users found, skipping import");
            return;
        }

        // Idempotent: skip if company already exists
        boolean companyExists = companyService.findAll(currentUser.getId(), currentUser.getRole()).stream()
                .anyMatch(c -> COMPANY_NAME.equalsIgnoreCase(c.name()));
        if (companyExists) {
            log.info("LinkedInJobImporter: company '{}' already exists, skipping import", COMPANY_NAME);
            return;
        }

        // 1. Create company
        CompanyForm companyForm = new CompanyForm();
        companyForm.setName(COMPANY_NAME);
        companyForm.setWebsite("https://feverup.com");
        companyForm.setNotes("World's leading tech platform for culture and live entertainment. Reaches 300M+ people in 55+ countries.");
        var company = companyService.create(companyForm, currentUser.getId());
        log.info("LinkedInJobImporter: created company '{}'", COMPANY_NAME);

        // 2. Project
        ProjectForm projectForm = new ProjectForm();
        projectForm.setName(JOB_TITLE);
        projectForm.setDescription("Imported from LinkedIn: " + JOB_URL);
        projectForm.setCompanyId(company.id());
        projectForm.setIndustries(java.util.Set.of("Software Development", "SaaS", "Cloud"));
        var project = projectService.create(projectForm, currentUser.getId(), currentUser.getRole());
        log.info("LinkedInJobImporter: created project '{}'", JOB_TITLE);

        // 3. Status
        Long statusId = pipelineStatusService.findByOwnerId(currentUser.getId()).stream()
                .findFirst()
                .map(PipelineStatusDto::id)
                .orElseThrow(() -> new IllegalStateException("No pipeline statuses found for user"));

        // 4. Job
        JobForm jobForm = new JobForm();
        jobForm.setPosition(JOB_TITLE);
        jobForm.setJobDescription("Python-based services/APIs, relational databases, data-warehouses, SQL, event-driven/distributed systems, cloud infrastructure, containers, CI/CD pipelines. May involve AI APIs, LLM workflows, RAG patterns.");
        jobForm.setJobUrl(JOB_URL);
        jobForm.setLocation("Spain (Valencia)");
        jobForm.setJobType(JobType.FULL_TIME);
        jobForm.setWorkType(WorkType.HYBRID);
        jobForm.setOnCallSchedule(OnCallSchedule.NONE);
        jobForm.setTimezone(JobTimezone.CET);
        jobForm.setSource(Source.LINKEDIN);
        jobForm.setNotes("Senior/Staff Product Engineer at Fever. Tech: Python, Django/Flask/FastAPI, PostgreSQL, Snowflake, Redis, Kafka, AWS, Docker, Kubernetes. 37 applicants.");
        jobForm.setProjectId(project.id());
        jobForm.setCurrentStatusId(statusId);
        var job = jobService.create(jobForm, currentUser.getId(), currentUser.getRole());
        log.info("LinkedInJobImporter: created job '{}' (id={})", JOB_TITLE, job.id());
    }
}
