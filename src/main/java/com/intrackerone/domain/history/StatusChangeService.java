package com.intrackerone.domain.history;

import com.intrackerone.domain.job.JobApplication;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.status.PipelineStatus;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
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
public class StatusChangeService {

    private final StatusChangeRepository statusChangeRepository;
    private final StatusChangeMapper statusChangeMapper;
    private final JobApplicationRepository jobRepository;
    private final PipelineStatusRepository pipelineStatusRepository;

    @Transactional(readOnly = true)
    public List<StatusChangeDto> findByJobId(Long jobId, Long currentUserId, Role role) {
        JobApplication job = findJob(jobId);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        return statusChangeRepository.findByJobApplicationIdOrderByChangedAtDesc(jobId).stream()
                .map(statusChangeMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatusChangeDto> findRecentByOwnerId(Long ownerId, int limit) {
        return statusChangeRepository.findByJobApplicationOwnerIdOrderByChangedAtDesc(ownerId).stream()
                .limit(limit)
                .map(statusChangeMapper::toDto)
                .toList();
    }

    @Transactional
    public StatusChangeDto recordChange(Long jobId, Long toStatusId, String decidedBy, String notes,
                                        Long currentUserId, Role role) {
        JobApplication job = findJob(jobId);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        PipelineStatus toStatus = findStatus(toStatusId);
        AccessControl.requireSameOwner(job.getOwner(), toStatus.getOwner(), "PipelineStatus");

        PipelineStatus fromStatus = job.getCurrentStatus();
        job.setCurrentStatus(toStatus);
        jobRepository.saveAndFlush(job);

        StatusChange change = statusChangeRepository.saveAndFlush(
                buildChange(job, fromStatus, toStatus, decidedBy, notes));
        log.info("Status changed for job {}: {} -> {}", jobId,
                fromStatus != null ? fromStatus.getName() : "NONE", toStatus.getName());
        return statusChangeMapper.toDto(change);
    }

    private StatusChange buildChange(JobApplication job, PipelineStatus fromStatus, PipelineStatus toStatus,
                                      String decidedBy, String notes) {
        return StatusChange.builder()
                .jobApplication(job)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedAt(LocalDateTime.now())
                .decidedBy(decidedBy)
                .notes(notes)
                .build();
    }

    private JobApplication findJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JobApplication", jobId));
    }

    private PipelineStatus findStatus(Long statusId) {
        return pipelineStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResourceNotFoundException("PipelineStatus", statusId));
    }
}
