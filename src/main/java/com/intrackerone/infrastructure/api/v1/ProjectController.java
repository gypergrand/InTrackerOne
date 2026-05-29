package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.project.ProjectDto;
import com.intrackerone.domain.project.ProjectForm;
import com.intrackerone.domain.project.ProjectService;
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
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SecurityService securityService;

    @GetMapping
    public List<ProjectDto> findByCompany(@RequestParam(required = false) Long companyId) {
        User current = securityService.getCurrentUser();
        if (companyId != null) {
            return projectService.findByCompanyId(companyId, current.getId(), current.getRole());
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public ProjectDto findById(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        return projectService.findById(id, current.getId(), current.getRole());
    }

    @PostMapping
    public ResponseEntity<ProjectDto> create(@Valid @RequestBody ProjectForm form) {
        User current = securityService.getCurrentUser();
        ProjectDto dto = projectService.create(form, current.getId(), current.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ProjectDto update(@PathVariable Long id, @Valid @RequestBody ProjectForm form) {
        User current = securityService.getCurrentUser();
        return projectService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        projectService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }
}
