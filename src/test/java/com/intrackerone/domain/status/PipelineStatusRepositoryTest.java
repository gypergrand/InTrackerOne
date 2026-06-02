package com.intrackerone.domain.status;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class PipelineStatusRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PipelineStatusRepository pipelineStatusRepository;

    private User user;
    private PipelineStatus applied;
    private PipelineStatus interview;
    private PipelineStatus rejected;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(User.builder()
            .username("statusowner")
            .password("pass")
            .email("status@test.com")
            .enabled(true)
            .role(Role.USER)
            .build());

        applied = em.persistAndFlush(PipelineStatus.builder()
            .name("Applied").sortOrder(1).color("#FF5733").type(StatusType.ACTIVE)
            .owner(user).isDefault(false).build());

        interview = em.persistAndFlush(PipelineStatus.builder()
            .name("Interview").sortOrder(2).color("#33FF57").type(StatusType.ACTIVE)
            .owner(user).isDefault(false).build());

        rejected = em.persistAndFlush(PipelineStatus.builder()
            .name("Rejected").sortOrder(3).color("#3357FF").type(StatusType.TERMINAL)
            .owner(user).isDefault(false).build());
    }

    @Test
    void findByOwnerIdOrderBySortOrderAsc_returnsOrdered() {
        List<PipelineStatus> result = pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(user.getId());

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("Applied");
        assertThat(result.get(1).getName()).isEqualTo("Interview");
        assertThat(result.get(2).getName()).isEqualTo("Rejected");
    }

    @Test
    void findByOwnerIdOrderBySortOrderAsc_returnsOnlyOwners() {
        User otherUser = em.persistAndFlush(User.builder()
            .username("otherstatus")
            .password("pass")
            .email("other@test.com")
            .enabled(true)
            .role(Role.USER)
            .build());
        em.persistAndFlush(PipelineStatus.builder()
            .name("Other Applied").sortOrder(1).color("#000000").type(StatusType.ACTIVE)
            .owner(otherUser).isDefault(false).build());

        List<PipelineStatus> result = pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(user.getId());

        assertThat(result).hasSize(3);
    }

    @Test
    void findByIsDefaultTrueOrderBySortOrderAsc_returnsDefaultStatuses() {
        em.persistAndFlush(PipelineStatus.builder()
            .name("Default Open").sortOrder(1).color("#AAAAAA").type(StatusType.ACTIVE)
            .owner(null).isDefault(true).build());
        em.persistAndFlush(PipelineStatus.builder()
            .name("Default Closed").sortOrder(2).color("#BBBBBB").type(StatusType.TERMINAL)
            .owner(null).isDefault(true).build());

        List<PipelineStatus> result = pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Default Open");
    }

    @Test
    void findByIsDefaultTrueOrderBySortOrderAsc_whenNone_returnsEmpty() {
        List<PipelineStatus> result = pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc();

        assertThat(result).isEmpty();
    }

    @Test
    void existsByOwnerIdAndName_whenExists_returnsTrue() {
        boolean exists = pipelineStatusRepository.existsByOwnerIdAndName(user.getId(), "Applied");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByOwnerIdAndName_whenNotExists_returnsFalse() {
        boolean exists = pipelineStatusRepository.existsByOwnerIdAndName(user.getId(), "Ghosted");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByOwnerIdAndName_differentOwner_returnsFalse() {
        boolean exists = pipelineStatusRepository.existsByOwnerIdAndName(999L, "Applied");

        assertThat(exists).isFalse();
    }
}
