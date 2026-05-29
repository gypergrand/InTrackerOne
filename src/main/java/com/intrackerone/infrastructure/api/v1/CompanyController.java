package com.intrackerone.infrastructure.api.v1;

import com.intrackerone.domain.company.CompanyDto;
import com.intrackerone.domain.company.CompanyForm;
import com.intrackerone.domain.company.CompanyService;
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
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final SecurityService securityService;

    @GetMapping
    public List<CompanyDto> findAll() {
        User current = securityService.getCurrentUser();
        return companyService.findAll(current.getId(), current.getRole());
    }

    @GetMapping("/{id}")
    public CompanyDto findById(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        return companyService.findById(id, current.getId(), current.getRole());
    }

    @PostMapping
    public ResponseEntity<CompanyDto> create(@Valid @RequestBody CompanyForm form) {
        User current = securityService.getCurrentUser();
        CompanyDto dto = companyService.create(form, current.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public CompanyDto update(@PathVariable Long id, @Valid @RequestBody CompanyForm form) {
        User current = securityService.getCurrentUser();
        return companyService.update(id, form, current.getId(), current.getRole());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User current = securityService.getCurrentUser();
        companyService.delete(id, current.getId(), current.getRole());
        return ResponseEntity.noContent().build();
    }
}
