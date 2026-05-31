package com.intrackerone.domain.recruiter;

import com.intrackerone.domain.company.Company;
import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.project.Project;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecruiterService {

    private final RecruiterRepository recruiterRepository;
    private final RecruiterMapper recruiterMapper;
    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public List<RecruiterDto> findByCompanyId(Long companyId, Long currentUserId, Role role) {
        if (role == Role.ADMIN || role == Role.MODERATOR) {
            return recruiterRepository.findByCompanyId(companyId).stream()
                    .map(recruiterMapper::toDto)
                    .toList();
        }
        return recruiterRepository.findByCompanyIdAndOwnerId(companyId, currentUserId).stream()
                .map(recruiterMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecruiterDto> findAllAccessible(Long currentUserId, Role role) {
        if (role == Role.ADMIN || role == Role.MODERATOR) {
            return recruiterRepository.findAll().stream()
                    .map(recruiterMapper::toDto)
                    .toList();
        }
        return recruiterRepository.findByOwnerId(currentUserId).stream()
                .map(recruiterMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecruiterDto findById(Long id, Long currentUserId, Role role) {
        Recruiter recruiter = findRecruiter(id);
        AccessControl.requireOwnerOrGlobal(recruiter.getOwner(), currentUserId, role, "Recruiter");
        return recruiterMapper.toDto(recruiter);
    }

    @Transactional
    public RecruiterDto create(RecruiterForm form, Long currentUserId, Role role) {
        Company company = findCompany(form.getCompanyId());
        AccessControl.requireOwnerOrGlobal(company.getOwner(), currentUserId, role, "Company");
        Recruiter recruiter = recruiterMapper.toEntity(form);
        recruiter.setCompany(company);
        recruiter.setOwner(company.getOwner());
        recruiter.setProjects(resolveProjects(form.getProjectIds(), company.getOwner(), company.getId()));
        recruiter = recruiterRepository.save(recruiter);
        log.info("Created recruiter: {} for company {}", recruiter.getName(), company.getName());
        return recruiterMapper.toDto(recruiter);
    }

    @Transactional
    public RecruiterDto update(Long id, RecruiterForm form, Long currentUserId, Role role) {
        Recruiter recruiter = findRecruiter(id);
        AccessControl.requireOwnerOrGlobal(recruiter.getOwner(), currentUserId, role, "Recruiter");
        updateFields(recruiter, form);
        updateCompany(recruiter, form.getCompanyId());
        if (form.getProjectIds() != null) {
            Long companyId = recruiter.getCompany().getId();
            recruiter.setProjects(resolveProjects(form.getProjectIds(), recruiter.getOwner(), companyId));
        }
        recruiter = recruiterRepository.save(recruiter);
        return recruiterMapper.toDto(recruiter);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        Recruiter recruiter = findRecruiter(id);
        AccessControl.requireOwnerOrGlobal(recruiter.getOwner(), currentUserId, role, "Recruiter");
        recruiterRepository.delete(recruiter);
    }

    private void updateFields(Recruiter recruiter, RecruiterForm form) {
        recruiter.setName(form.getName());
        recruiter.setEmail(form.getEmail());
        recruiter.setPhone(form.getPhone());
        recruiter.setNotes(form.getNotes());
    }

    private void updateCompany(Recruiter recruiter, Long companyId) {
        if (companyId == null) {
            return;
        }
        Company company = findCompany(companyId);
        AccessControl.requireSameOwner(recruiter.getOwner(), company.getOwner(), "Company");
        recruiter.setCompany(company);
    }

    private Set<Project> resolveProjects(Set<Long> projectIds, User owner, Long companyId) {
        if (projectIds == null || projectIds.isEmpty()) {
            return Set.of();
        }
        return projectIds.stream()
                .map(pid -> resolveProject(pid, owner, companyId))
                .collect(Collectors.toSet());
    }

    private Project resolveProject(Long projectId, User owner, Long companyId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        AccessControl.requireSameOwner(owner, project.getOwner(), "Project");
        requireProjectCompany(project, companyId);
        return project;
    }

    private void requireProjectCompany(Project project, Long companyId) {
        if (project.getCompany().getId().equals(companyId)) {
            return;
        }
        throw new UnauthorizedException("Project belongs to another company");
    }

    private Recruiter findRecruiter(Long id) {
        return recruiterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter", id));
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }
}
