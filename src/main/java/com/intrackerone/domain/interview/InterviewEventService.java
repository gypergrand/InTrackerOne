package com.intrackerone.domain.interview;

import com.intrackerone.domain.job.JobApplication;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewEventService {

    private final InterviewEventRepository interviewEventRepository;
    private final InterviewEventMapper interviewEventMapper;
    private final JobApplicationRepository jobRepository;

    @Transactional(readOnly = true)
    public List<InterviewEventDto> findByJobId(Long jobId, Long currentUserId, Role role) {
        JobApplication job = findJob(jobId);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        return interviewEventRepository.findByJobApplicationIdOrderByEventDateAsc(jobId).stream()
            .map(interviewEventMapper::toDto)
            .toList();
    }

    @Transactional
    public InterviewEventDto create(Long jobId, InterviewEventForm form, Long currentUserId, Role role) {
        JobApplication job = findJob(jobId);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        InterviewEvent event = interviewEventMapper.toEntity(form);
        event.setJobApplication(job);
        event = interviewEventRepository.save(event);
        log.info("Created interview event '{}' for job {}", form.getTitle(), jobId);
        return interviewEventMapper.toDto(event);
    }

    @Transactional
    public InterviewEventDto update(Long id, InterviewEventForm form, Long currentUserId, Role role) {
        InterviewEvent event = findEvent(id);
        AccessControl.requireOwnerOrGlobal(event.getJobApplication().getOwner(), currentUserId, role, "InterviewEvent");
        event.setTitle(form.getTitle());
        event.setEventDate(form.getEventDate());
        event.setDurationMinutes(form.getDurationMinutes());
        event.setFormat(form.getFormat());
        event.setInterviewerName(form.getInterviewerName());
        event.setNotes(form.getNotes());
        event = interviewEventRepository.save(event);
        return interviewEventMapper.toDto(event);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        InterviewEvent event = findEvent(id);
        AccessControl.requireOwnerOrGlobal(event.getJobApplication().getOwner(), currentUserId, role, "InterviewEvent");
        interviewEventRepository.delete(event);
    }

    private JobApplication findJob(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("JobApplication", jobId));
    }

    private InterviewEvent findEvent(Long id) {
        return interviewEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("InterviewEvent", id));
    }
}
