package com.intrackerone.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class ApiTokenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ApiTokenRepository apiTokenRepository;

    private User user;
    private ApiToken token;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(User.builder()
            .username("tokenuser")
            .password("pass")
            .email("token@test.com")
            .enabled(true)
            .role(Role.USER)
            .build());

        token = em.persistAndFlush(ApiToken.builder()
            .name("Test Token")
            .tokenHash("abc123".repeat(10))
            .tokenIdentifier("test-identifier-1")
            .user(user)
            .enabled(true)
            .build());
    }

    @Test
    void findByUserId_returnsTokensForUser() {
        List<ApiToken> result = apiTokenRepository.findByUserId(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Test Token");
    }

    @Test
    void findByUserId_returnsEmptyForOtherUser() {
        User otherUser = em.persistAndFlush(User.builder()
            .username("other")
            .password("pass")
            .email("other@test.com")
            .enabled(true)
            .role(Role.USER)
            .build());

        List<ApiToken> result = apiTokenRepository.findByUserId(otherUser.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findByTokenIdentifier_whenExists_returnsTokenWithUserFetched() {
        Optional<ApiToken> result = apiTokenRepository.findByTokenIdentifier("test-identifier-1");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Token");
        assertThat(result.get().getUser().getUsername()).isEqualTo("tokenuser");
    }

    @Test
    void findByTokenIdentifier_whenNotExists_returnsEmpty() {
        Optional<ApiToken> result = apiTokenRepository.findByTokenIdentifier("nonexistent");

        assertThat(result).isEmpty();
    }
}
