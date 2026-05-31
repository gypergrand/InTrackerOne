package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.user.ApiTokenService;
import com.intrackerone.infrastructure.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
public class TokenController {

    private final ApiTokenService apiTokenService;
    private final SecurityService securityService;

    @GetMapping
    public List<?> findAll() {
        var current = securityService.getCurrentUser();
        return apiTokenService.findByUserId(current.getId());
    }

    @PostMapping
    public ResponseEntity<ApiTokenService.CreatedToken> create(@RequestBody Map<String, String> body) {
        var current = securityService.getCurrentUser();
        ApiTokenService.CreatedToken result = apiTokenService.create(current.getId(), body.get("name"));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        var current = securityService.getCurrentUser();
        apiTokenService.revoke(id, current.getId());
        return ResponseEntity.noContent().build();
    }
}
