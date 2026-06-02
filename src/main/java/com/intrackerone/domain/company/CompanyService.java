package com.intrackerone.domain.company;

import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final RecruiterRepository recruiterRepository;

    @Transactional(readOnly = true)
    public List<CompanyDto> findAll(Long currentUserId, Role role) {
        if (role == Role.ADMIN || role == Role.MODERATOR) {
            return companyRepository.findAllByOrderByNameAsc().stream()
                    .map(companyMapper::toDto)
                    .toList();
        }
        return companyRepository.findByOwnerId(currentUserId).stream()
                .map(companyMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDto findById(Long id, Long currentUserId, Role role) {
        Company company = findCompany(id);
        AccessControl.requireOwnerOrGlobal(company.getOwner(), currentUserId, role, "Company");
        return companyMapper.toDto(company);
    }

    @Transactional
    public CompanyDto create(CompanyForm form, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));
        Company company = companyMapper.toEntity(form);
        company.setOwner(owner);
        company = companyRepository.save(company);
        log.info("Created company: {}", company.getName());
        return companyMapper.toDto(company);
    }

    @Transactional
    public CompanyDto update(Long id, CompanyForm form, Long currentUserId, Role role) {
        return update(id, form, null, currentUserId, role);
    }

    @Transactional
    public CompanyDto update(Long id, CompanyForm form, Long newOwnerId, Long currentUserId, Role role) {
        Company company = findCompany(id);
        AccessControl.requireOwnerOrGlobal(company.getOwner(), currentUserId, role, "Company");
        company.setName(form.getName());
        company.setWebsite(form.getWebsite());
        company.setNotes(form.getNotes());
        if (newOwnerId != null && !newOwnerId.equals(company.getOwner().getId())) {
            if (!AccessControl.hasGlobalDataAccess(role)) {
                throw new UnauthorizedException("Only ADMIN or MODERATOR can transfer company ownership");
            }
            User newOwner = userRepository.findById(newOwnerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", newOwnerId));
            company.setOwner(newOwner);
        }
        company = companyRepository.save(company);
        return companyMapper.toDto(company);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        Company company = findCompany(id);
        AccessControl.requireOwnerOrGlobal(company.getOwner(), currentUserId, role, "Company");
        if (!projectRepository.findByCompanyId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete company with existing projects. Delete projects first.");
        }
        if (!recruiterRepository.findByCompanyId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete company with existing recruiters. Delete recruiters first.");
        }
        companyRepository.delete(company);
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }
}
