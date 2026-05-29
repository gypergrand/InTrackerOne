package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.user.ApiTokenService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.security.SecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class TokenController {

    private final ApiTokenService apiTokenService;
    private final SecurityService securityService;

    @GetMapping
    public List<?> findAll() {
        User current = securityService.getCurrentUser();
        if (current.getRole() == Role.USER) {
            throw new SecurityException("Only admins and moderators can manage API tokens");
        }
        return apiTokenService.findByUserId(current.getId());
    }

    @PostMapping
    public ResponseEntity<ApiTokenService.CreatedToken> create(@RequestBody Map<String, String> body) {
        User current = securityService.getCurrentUser();
        if (current.getRole() == Role.USER) {
            throw new SecurityException("Only admins and moderators can create API tokens");
        }
        ApiTokenService.CreatedToken result = apiTokenService.create(current.getId(), body.get("name"));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        if (current.getRole() == Role.USER) {
            throw new SecurityException("Only admins and moderators can manage API tokens");
        }
        apiTokenService.revoke(id, current.getId());
        return ResponseEntity.noContent().build();
    }
}
