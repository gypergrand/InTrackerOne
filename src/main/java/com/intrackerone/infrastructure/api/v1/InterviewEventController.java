package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.interview.InterviewEventDto;
import com.intrackerone.domain.interview.InterviewEventForm;
import com.intrackerone.domain.interview.InterviewEventService;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InterviewEventController {

    private final InterviewEventService interviewEventService;
    private final SecurityService securityService;

    @GetMapping("/jobs/{jobId}/events")
    public List<InterviewEventDto> findByJob(@PathVariable Long jobId) {
        User current = securityService.getCurrentUser();
        return interviewEventService.findByJobId(jobId, current.getId(), current.getRole());
    }

    @PostMapping("/jobs/{jobId}/events")
    public ResponseEntity<InterviewEventDto> create(@PathVariable Long jobId,
                                                    @Valid @RequestBody InterviewEventForm form) {
        User current = securityService.getCurrentUser();
        InterviewEventDto dto = interviewEventService.create(jobId, form, current.getId(), current.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/events/{id}")
    public InterviewEventDto update(@PathVariable Long id, @Valid @RequestBody InterviewEventForm form) {
        User current = securityService.getCurrentUser();
        return interviewEventService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        interviewEventService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }
}
