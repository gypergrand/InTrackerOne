package com.intrackerone.domain.compensation;

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
public class CompensationEntryService {

    private final CompensationEntryRepository compensationEntryRepository;
    private final CompensationEntryMapper compensationEntryMapper;
    private final JobApplicationRepository jobRepository;

    @Transactional(readOnly = true)
    public List<CompensationEntryDto> findByJobId(Long jobId, Long currentUserId, Role role) {
        JobApplication job = findJob(jobId);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        return compensationEntryRepository.findByJobApplicationId(jobId).stream()
            .map(compensationEntryMapper::toDto)
            .toList();
    }

    @Transactional
    public CompensationEntryDto create(Long jobId, CompensationEntryForm form, Long currentUserId, Role role) {
        JobApplication job = findJob(jobId);
        AccessControl.requireOwnerOrGlobal(job.getOwner(), currentUserId, role, "JobApplication");
        CompensationEntry entry = compensationEntryMapper.toEntity(form);
        entry.setJobApplication(job);
        entry = compensationEntryRepository.save(entry);
        return compensationEntryMapper.toDto(entry);
    }

    @Transactional
    public CompensationEntryDto update(Long id, CompensationEntryForm form, Long currentUserId, Role role) {
        CompensationEntry entry = findEntry(id);
        AccessControl.requireOwnerOrGlobal(entry.getJobApplication().getOwner(), currentUserId, role, "CompensationEntry");
        entry.setRateType(form.getRateType());
        entry.setAmount(form.getAmount());
        entry.setCurrency(form.getCurrency());
        entry.setCategory(form.getCategory());
        entry.setNotes(form.getNotes());
        entry = compensationEntryRepository.save(entry);
        return compensationEntryMapper.toDto(entry);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        CompensationEntry entry = findEntry(id);
        AccessControl.requireOwnerOrGlobal(entry.getJobApplication().getOwner(), currentUserId, role, "CompensationEntry");
        compensationEntryRepository.delete(entry);
    }

    private JobApplication findJob(Long jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("JobApplication", jobId));
    }

    private CompensationEntry findEntry(Long id) {
        return compensationEntryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("CompensationEntry", id));
    }
}
