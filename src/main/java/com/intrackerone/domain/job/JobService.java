package com.intrackerone.domain.job;

import com.intrackerone.domain.history.StatusChange;
import com.intrackerone.domain.history.StatusChangeRepository;
import com.intrackerone.domain.project.Project;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.recruiter.Recruiter;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.status.PipelineStatus;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobService {

    private final JobApplicationRepository jobRepository;
    private final JobMapper jobMapper;
    private final ProjectRepository projectRepository;
    private final RecruiterRepository recruiterRepository;
    private final PipelineStatusRepository pipelineStatusRepository;
    private final StatusChangeRepository statusChangeRepository;
    private final com.intrackerone.domain.user.UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<JobDto> findAll(Long currentUserId, Role role, Long statusId, Long companyId, Long projectId) {
        List<JobApplication> jobs;
        if (role == Role.ADMIN) {
            jobs = jobRepository.findAll();
        } else {
            jobs = jobRepository.findByOwnerId(currentUserId);
        }
        return jobs.stream()
                .filter(j -> statusId == null || (j.getCurrentStatus() != null && j.getCurrentStatus().getId().equals(statusId)))
                .filter(j -> companyId == null || (j.getProject() != null && j.getProject().getCompany() != null
                        && j.getProject().getCompany().getId().equals(companyId)))
                .filter(j -> projectId == null || (j.getProject() != null && j.getProject().getId().equals(projectId)))
                .map(jobMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDto findById(Long id, Long currentUserId, Role role) {
        JobApplication job = findJob(id);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        return jobMapper.toDto(job);
    }

    @Transactional
    public JobDto create(JobForm form, Long currentUserId, Role role) {
        Project project = findProject(form.getProjectId());
        AccessControl.requireOwnerOrGlobal(project.getOwner(), currentUserId, role, "Project");
        PipelineStatus status = findStatus(form.getCurrentStatusId());
        User currentUser = getUser(currentUserId);
        requireSameOwner(currentUser, status.getOwner(), "PipelineStatus");

        JobApplication job = jobMapper.toEntity(form);
        job.setProject(project);
        job.setCurrentStatus(status);
        job.setOwner(currentUser);
        updateRecruiter(job, form.getRecruiterId(), project, currentUser);
        job = jobRepository.saveAndFlush(job);

        StatusChange change = StatusChange.builder()
                .jobApplication(job)
                .fromStatus(null)
                .toStatus(status)
                .changedAt(LocalDateTime.now())
                .build();
        statusChangeRepository.saveAndFlush(change);

        log.info("Created job application: {} ({})", job.getPosition(), project.getName());
        return jobMapper.toDto(job);
    }

    @Transactional
    public JobDto update(Long id, JobForm form, Long currentUserId, Role role) {
        JobApplication job = findJob(id);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        updateFields(job, form);
        Project project = updateProject(job, form.getProjectId());
        updateStatus(job, form.getCurrentStatusId());
        updateRecruiter(job, form.getRecruiterId(), project, job.getOwner());
        job = jobRepository.saveAndFlush(job);
        return jobMapper.toDto(job);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        JobApplication job = findJob(id);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        jobRepository.delete(job);
    }

    @Transactional
    public long countByOwnerId(Long ownerId) {
        return jobRepository.countByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public long countByOwnerIdAndStatusId(Long ownerId, Long statusId) {
        return jobRepository.countByOwnerIdAndCurrentStatusId(ownerId, statusId);
    }

    private void updateFields(JobApplication job, JobForm form) {
        job.setPosition(form.getPosition());
        job.setJobDescription(form.getJobDescription());
        job.setJobUrl(form.getJobUrl());
        job.setLocation(form.getLocation());
        job.setSalaryRange(form.getSalaryRange());
        job.setJobType(form.getJobType());
        job.setSource(form.getSource());
        job.setSourceMeta(form.getSourceMeta());
        job.setNotes(form.getNotes());
    }

    private Project updateProject(JobApplication job, Long projectId) {
        if (projectId == null) {
            return job.getProject();
        }
        Project project = findProject(projectId);
        requireSameOwner(job.getOwner(), project.getOwner(), "Project");
        job.setProject(project);
        return project;
    }

    private void updateStatus(JobApplication job, Long statusId) {
        if (statusId == null) {
            return;
        }
        PipelineStatus newStatus = findStatus(statusId);
        requireSameOwner(job.getOwner(), newStatus.getOwner(), "PipelineStatus");
        PipelineStatus oldStatus = job.getCurrentStatus();

        // Record status change in history when the status actually changes
        boolean statusChanged = oldStatus == null || !oldStatus.getId().equals(newStatus.getId());
        job.setCurrentStatus(newStatus);
        if (statusChanged) {
            StatusChange change = StatusChange.builder()
                    .jobApplication(job)
                    .fromStatus(oldStatus)
                    .toStatus(newStatus)
                    .changedAt(LocalDateTime.now())
                    .build();
            statusChangeRepository.saveAndFlush(change);
            log.info("Status changed for job {}: {} -> {}",
                    job.getId(),
                    oldStatus != null ? oldStatus.getName() : "NONE",
                    newStatus.getName());
        }
    }

    private void updateRecruiter(JobApplication job, Long recruiterId, Project project, User owner) {
        if (recruiterId == null) {
            job.setRecruiter(null);
            return;
        }
        Recruiter recruiter = findRecruiter(recruiterId);
        requireSameOwner(owner, recruiter.getOwner(), "Recruiter");
        requireRecruiterCompany(recruiter, project);
        job.setRecruiter(recruiter);
    }

    private void requireRecruiterCompany(Recruiter recruiter, Project project) {
        if (recruiter.getCompany().getId().equals(project.getCompany().getId())) {
            return;
        }
        throw new UnauthorizedException("Recruiter belongs to another company");
    }

    private void requireSameOwner(User expectedOwner, User actualOwner, String resource) {
        AccessControl.requireSameOwner(expectedOwner, actualOwner, resource);
    }

    private JobApplication findJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", id));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private PipelineStatus findStatus(Long id) {
        return pipelineStatusRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineStatus", id));
    }

    private Recruiter findRecruiter(Long id) {
        return recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter", id));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
