package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.status.PipelineStatusDto;
import com.intrackerone.domain.status.PipelineStatusForm;
import com.intrackerone.domain.status.PipelineStatusService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statuses")
@RequiredArgsConstructor
public class StatusController {

    private final PipelineStatusService pipelineStatusService;
    private final SecurityService securityService;

    @GetMapping
    public List<PipelineStatusDto> findAll() {
        User current = securityService.getCurrentUser();
        return pipelineStatusService.findByOwnerId(current.getId());
    }

    @GetMapping("/{id}")
    public PipelineStatusDto findById(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        return pipelineStatusService.findById(id, current.getId(), current.getRole());
    }

    @PostMapping
    public ResponseEntity<PipelineStatusDto> create(@Valid @RequestBody PipelineStatusForm form) {
        User current = securityService.getCurrentUser();
        PipelineStatusDto dto = pipelineStatusService.create(form, current.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public PipelineStatusDto update(@PathVariable Long id, @Valid @RequestBody PipelineStatusForm form) {
        User current = securityService.getCurrentUser();
        return pipelineStatusService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        pipelineStatusService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<Void> move(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        User current = securityService.getCurrentUser();
        pipelineStatusService.move(id, body.get("newOrder"), current.getId(), current.getRole());
        return ResponseEntity.ok().build();
    }
}
