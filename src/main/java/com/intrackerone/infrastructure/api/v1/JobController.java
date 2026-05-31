package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.history.StatusChangeService;
import com.intrackerone.domain.job.JobDto;
import com.intrackerone.domain.job.JobForm;
import com.intrackerone.domain.job.JobService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.security.SecurityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final StatusChangeService statusChangeService;
    private final SecurityService securityService;

    public record StatusChangeRequest(
            @NotNull Long statusId,
            String notes,
            String decidedBy
    ) {}

    @GetMapping
    public List<JobDto> findAll(@RequestParam(required = false) Long status,
                                @RequestParam(required = false) Long company,
                                @RequestParam(required = false) Long project) {
        User current = securityService.getCurrentUser();
        return jobService.findAll(current.getId(), current.getRole(), status, company, project);
    }

    @GetMapping("/{id}")
    public JobDto findById(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        return jobService.findById(id, current.getId(), current.getRole());
    }

    @PostMapping
    public ResponseEntity<JobDto> create(@Valid @RequestBody JobForm form) {
        User current = securityService.getCurrentUser();
        JobDto dto = jobService.create(form, current.getId(), current.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public JobDto update(@PathVariable Long id, @Valid @RequestBody JobForm form) {
        User current = securityService.getCurrentUser();
        return jobService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        jobService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable Long id,
                                             @Valid @RequestBody StatusChangeRequest request) {
        User current = securityService.getCurrentUser();
        statusChangeService.recordChange(id, request.statusId(), request.decidedBy(),
                request.notes(), current.getId(), current.getRole());
        return ResponseEntity.ok().build();
    }
}
