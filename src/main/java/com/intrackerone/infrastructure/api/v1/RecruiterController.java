package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.recruiter.RecruiterDto;
import com.intrackerone.domain.recruiter.RecruiterForm;
import com.intrackerone.domain.recruiter.RecruiterService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
public class RecruiterController {

    private final RecruiterService recruiterService;
    private final SecurityService securityService;

    @GetMapping
    public List<RecruiterDto> findByCompany(@RequestParam(required = false) Long companyId) {
        User current = securityService.getCurrentUser();
        if (companyId != null) {
            return recruiterService.findByCompanyId(companyId, current.getId(), current.getRole());
        }
        return recruiterService.findAllAccessible(current.getId(), current.getRole());
    }

    @GetMapping("/{id}")
    public RecruiterDto findById(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        return recruiterService.findById(id, current.getId(), current.getRole());
    }

    @PostMapping
    public ResponseEntity<RecruiterDto> create(@Valid @RequestBody RecruiterForm form) {
        User current = securityService.getCurrentUser();
        RecruiterDto dto = recruiterService.create(form, current.getId(), current.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public RecruiterDto update(@PathVariable Long id, @Valid @RequestBody RecruiterForm form) {
        User current = securityService.getCurrentUser();
        return recruiterService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        recruiterService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }
}
