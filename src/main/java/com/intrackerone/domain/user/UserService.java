package com.intrackerone.domain.user;

import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
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
        userRepository.deleteById(id);
    }
}
