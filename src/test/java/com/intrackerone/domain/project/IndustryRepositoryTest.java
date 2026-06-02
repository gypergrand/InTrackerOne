package com.intrackerone.domain.project;

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
class IndustryRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private IndustryRepository industryRepository;

    private Industry software;
    private Industry hardware;

    @BeforeEach
    void setUp() {
        software = em.persistAndFlush(Industry.builder().name("Software Development").build());
        hardware = em.persistAndFlush(Industry.builder().name("Hardware Engineering").build());
    }

    @Test
    void findByNameIgnoreCase_whenExists_returnsIndustry() {
        Optional<Industry> result = industryRepository.findByNameIgnoreCase("software development");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Software Development");
    }

    @Test
    void findByNameIgnoreCase_whenNotExists_returnsEmpty() {
        Optional<Industry> result = industryRepository.findByNameIgnoreCase("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void findByNameIgnoreCase_caseInsensitive() {
        Optional<Industry> result = industryRepository.findByNameIgnoreCase("SOFTWARE DEVELOPMENT");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(software.getId());
    }

    @Test
    void findByNameContainingIgnoreCaseOrderByNameAsc_returnsMatching() {
        em.persistAndFlush(Industry.builder().name("Cloud Computing").build());

        List<Industry> result = industryRepository.findByNameContainingIgnoreCaseOrderByNameAsc("software");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Software Development");
    }

    @Test
    void findByNameContainingIgnoreCaseOrderByNameAsc_caseInsensitive() {
        List<Industry> result = industryRepository.findByNameContainingIgnoreCaseOrderByNameAsc("HARDWARE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Hardware Engineering");
    }

    @Test
    void findByNameContainingIgnoreCaseOrderByNameAsc_returnsSorted() {
        em.persistAndFlush(Industry.builder().name("Data Analytics").build());
        em.persistAndFlush(Industry.builder().name("Data Science").build());

        List<Industry> result = industryRepository.findByNameContainingIgnoreCaseOrderByNameAsc("data");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Data Analytics");
        assertThat(result.get(1).getName()).isEqualTo("Data Science");
    }

    @Test
    void findByIdIn_returnsMatchingIndustries() {
        List<Industry> result = industryRepository.findByIdIn(List.of(software.getId(), hardware.getId()));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByIdIn_withPartialMatch() {
        Industry cloud = em.persistAndFlush(Industry.builder().name("Cloud").build());

        List<Industry> result = industryRepository.findByIdIn(List.of(software.getId(), cloud.getId()));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByIdIn_withEmptyList_returnsEmpty() {
        List<Industry> result = industryRepository.findByIdIn(List.of());

        assertThat(result).isEmpty();
    }
}
