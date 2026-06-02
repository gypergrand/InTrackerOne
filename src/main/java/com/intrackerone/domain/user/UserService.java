package com.intrackerone.domain.user;

import com.intrackerone.domain.company.CompanyRepository;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.project.ProjectRepository;
import com.intrackerone.domain.recruiter.RecruiterRepository;
import com.intrackerone.domain.status.PipelineStatusRepository;
import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import com.intrackerone.infrastructure.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PipelineStatusService pipelineStatusService;
    private final ApiTokenRepository apiTokenRepository;
    private final CompanyRepository companyRepository;
    private final ProjectRepository projectRepository;
    private final RecruiterRepository recruiterRepository;
    private final PipelineStatusRepository pipelineStatusRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
            .map(userMapper::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        return userRepository.findById(id)
            .map(userMapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional
    public UserDto create(UserForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        User user = userMapper.toEntity(form);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user = userRepository.save(user);
        pipelineStatusService.seedDefaultsForUser(user.getId());
        log.info("Created user: {}", user.getUsername());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto update(Long id, UserForm form) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setRole(form.getRole());
        if (form.getEnabled() != null) {
            user.setEnabled(form.getEnabled());
        }
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        if (!apiTokenRepository.findByUserId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete user with existing API tokens. Revoke tokens first.");
        }
        if (!companyRepository.findByOwnerId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete user with existing companies. Delete companies first.");
        }
        if (!projectRepository.findByOwnerId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete user with existing projects. Delete projects first.");
        }
        if (!recruiterRepository.findByOwnerId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete user with existing recruiters. Delete recruiters first.");
        }
        if (!pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete user with existing pipeline statuses. Delete statuses first.");
        }
        if (!jobApplicationRepository.findByOwnerId(id).isEmpty()) {
            throw new UnauthorizedException("Cannot delete user with existing job applications. Delete jobs first.");
        }
        userRepository.deleteById(id);
    }
}
