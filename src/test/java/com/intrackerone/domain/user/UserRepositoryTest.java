package com.intrackerone.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_whenExists_returnsUser() {
        userRepository.save(User.builder()
            .username("johndoe").password("encoded").email("john@test.com")
            .enabled(true).role(Role.USER).build());

        Optional<User> result = userRepository.findByUsername("johndoe");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("john@test.com");
    }

    @Test
    void findByUsername_whenNotFound_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_whenExists_returnsTrue() {
        userRepository.save(User.builder()
            .username("existing").password("pass").enabled(true).role(Role.ADMIN).build());

        boolean exists = userRepository.existsByUsername("existing");

        assertThat(exists).isTrue();
    }

    @Test
    void save_encodesTimestamps() {
        User user = userRepository.save(User.builder()
            .username("newuser").password("pass").enabled(true).role(Role.USER).build());

        assertThat(user.getId()).isNotNull();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }
}
