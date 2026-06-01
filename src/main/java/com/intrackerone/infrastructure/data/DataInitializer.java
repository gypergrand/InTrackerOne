package com.intrackerone.infrastructure.data;

import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PipelineStatusService pipelineStatusService;

    @Override
    public void run(String... args) {
        User admin = null;
        if (!userRepository.existsByUsername("admin")) {
            admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .email("admin@intrackerone.local")
                .enabled(true)
                .role(Role.ADMIN)
                .build();
            admin = userRepository.save(admin);
            log.info("Created admin user (default password: admin)");
        }

        if (!userRepository.existsByUsername("user")) {
            User regularUser = User.builder()
                .username("user")
                .password(passwordEncoder.encode("user"))
                .email("user@intrackerone.local")
                .enabled(true)
                .role(Role.USER)
                .build();
            regularUser = userRepository.save(regularUser);
            pipelineStatusService.seedDefaultsForUser(regularUser.getId());
            log.info("Created regular user (default password: user)");
        }

        if (admin != null) {
            pipelineStatusService.seedDefaultsForUser(admin.getId());
        }
        log.info("Data initialization complete");
    }
}
