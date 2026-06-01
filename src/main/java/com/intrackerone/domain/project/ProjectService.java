package com.intrackerone.domain.project;

import com.intrackerone.domain.company.Company;
import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.user.Role;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.security.AccessControl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final CompanyRepository companyRepository;
    private final IndustryRepository industryRepository;

    @Transactional(readOnly = true)
    public List<ProjectDto> findByCompanyId(Long companyId, Long currentUserId, Role role) {
        if (companyId == null) {
            return findAllAccessible(currentUserId, role);
        }
        if (role == Role.ADMIN || role == Role.MODERATOR) {
            return projectRepository.findByCompanyId(companyId).stream()
                    .map(projectMapper::toDto)
                    .toList();
        }
        return projectRepository.findByCompanyIdAndOwnerId(companyId, currentUserId).stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> findAllAccessible(Long currentUserId, Role role) {
        if (role == Role.ADMIN || role == Role.MODERATOR) {
            return projectRepository.findAll().stream()
                    .map(projectMapper::toDto)
                    .toList();
        }
        return projectRepository.findByOwnerId(currentUserId).stream()
                .map(projectMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto findById(Long id, Long currentUserId, Role role) {
        Project project = findProject(id);
        AccessControl.requireOwnerOrGlobal(project.getOwner(), currentUserId, role, "Project");
        return projectMapper.toDto(project);
    }

    @Transactional
    public ProjectDto create(ProjectForm form, Long currentUserId, Role role) {
        Company company = findCompany(form.getCompanyId());
        AccessControl.requireOwnerOrGlobal(company.getOwner(), currentUserId, role, "Company");
        Project project = projectMapper.toEntity(form);
        project.setCompany(company);
        project.setOwner(company.getOwner());
        project.setIndustries(resolveIndustries(form.getIndustries()));
        project = projectRepository.save(project);
        log.info("Created project: {} for company {}", project.getName(), company.getName());
        return projectMapper.toDto(project);
    }

    @Transactional
    public ProjectDto update(Long id, ProjectForm form, Long currentUserId, Role role) {
        Project project = findProject(id);
        AccessControl.requireOwnerOrGlobal(project.getOwner(), currentUserId, role, "Project");
        project.setName(form.getName());
        project.setDescription(form.getDescription());
        updateCompany(project, form.getCompanyId());
        project.setIndustries(resolveIndustries(form.getIndustries()));
        project = projectRepository.save(project);
        return projectMapper.toDto(project);
    }

    @Transactional
    public void delete(Long id, Long currentUserId, Role role) {
        Project project = findProject(id);
        AccessControl.requireOwnerOrGlobal(project.getOwner(), currentUserId, role, "Project");
        projectRepository.delete(project);
    }

    private void updateCompany(Project project, Long companyId) {
        if (companyId == null) {
            return;
        }
        Company company = findCompany(companyId);
        AccessControl.requireSameOwner(project.getOwner(), company.getOwner(), "Company");
        project.setCompany(company);
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private Company findCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Company", id));
    }

    /**
     * Resolves industry names to Industry entities.
     * Finds existing ones by name (case-insensitive) or creates new ones.
     */
    private java.util.Set<Industry> resolveIndustries(java.util.Set<String> industryNames) {
        if (industryNames == null || industryNames.isEmpty()) {
            return new java.util.HashSet<>();
        }
        return industryNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(name -> industryRepository.findByNameIgnoreCase(name.trim())
                        .orElseGet(() -> industryRepository.save(Industry.builder().name(name.trim()).build())))
                .collect(java.util.stream.Collectors.toSet());
    }
}
