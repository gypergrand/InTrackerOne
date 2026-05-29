package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserDto;
import com.intrackerone.domain.user.UserForm;
import com.intrackerone.domain.user.UserService;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityService securityService;

    @GetMapping
    public List<UserDto> findAll() {
        User current = securityService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can manage users");
        }
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserDto findById(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can manage users");
        }
        return userService.findById(id);
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserForm form) {
        User current = securityService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can manage users");
        }
        UserDto dto = userService.create(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserForm form) {
        User current = securityService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can manage users");
        }
        return userService.update(id, form);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        if (current.getRole() != Role.ADMIN) {
            throw new SecurityException("Only admins can manage users");
        }
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
