package com.intrackerone.infrastructure.security;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService jpaUserDetailsService;

    @Test
    void loadUserByUsername_whenExists_returnsUserDetails() {
        User user = User.builder()
            .username("admin")
            .password("encoded-password")
            .enabled(true)
            .role(Role.ADMIN)
            .build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails result = jpaUserDetailsService.loadUserByUsername("admin");

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_whenNotExists_throwsException() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jpaUserDetailsService.loadUserByUsername("nonexistent"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("nonexistent");
    }

    @Test
    void loadUserByUsername_forRegularUser_setsRole() {
        User user = User.builder()
            .username("user")
            .password("pass")
            .enabled(true)
            .role(Role.USER)
            .build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        UserDetails result = jpaUserDetailsService.loadUserByUsername("user");

        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void loadUserByUsername_whenDisabled_accountNotEnabled() {
        User user = User.builder()
            .username("disabled")
            .password("pass")
            .enabled(false)
            .role(Role.USER)
            .build();
        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(user));

        UserDetails result = jpaUserDetailsService.loadUserByUsername("disabled");

        assertThat(result.isEnabled()).isFalse();
    }
}
