package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.compensation.CompensationEntryDto;
import com.intrackerone.domain.compensation.CompensationEntryForm;
import com.intrackerone.domain.compensation.CompensationEntryService;
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
public class CompensationController {

    private final CompensationEntryService compensationEntryService;
    private final SecurityService securityService;

    @GetMapping("/jobs/{jobId}/compensation")
    public List<CompensationEntryDto> findByJob(@PathVariable Long jobId) {
        User current = securityService.getCurrentUser();
        return compensationEntryService.findByJobId(jobId, current.getId(), current.getRole());
    }

    @PostMapping("/jobs/{jobId}/compensation")
    public ResponseEntity<CompensationEntryDto> create(@PathVariable Long jobId,
                                                       @Valid @RequestBody CompensationEntryForm form) {
        User current = securityService.getCurrentUser();
        CompensationEntryDto dto = compensationEntryService.create(jobId, form, current.getId(), current.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/compensation/{id}")
    public CompensationEntryDto update(@PathVariable Long id, @Valid @RequestBody CompensationEntryForm form) {
        User current = securityService.getCurrentUser();
        return compensationEntryService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/compensation/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        compensationEntryService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }
}
