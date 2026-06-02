package com.intrackerone.domain.company;

import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
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
class CompanyRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
            .username("owner").password("pass").email("owner@test.com").enabled(true).role(Role.USER).build());
    }

    @Test
    void findByOwnerId_returnsOnlyOwnedCompanies() {
        companyRepository.save(Company.builder().name("Mine").owner(user).build());

        User otherUser = userRepository.save(User.builder()
            .username("other").password("pass").email("other@test.com").enabled(true).role(Role.USER).build());
        companyRepository.save(Company.builder().name("Theirs").owner(otherUser).build());

        List<Company> result = companyRepository.findByOwnerId(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Mine");
    }

    @Test
    void findAllByOrderByNameAsc_returnsSorted() {
        companyRepository.save(Company.builder().name("Zeta Corp").owner(user).build());
        companyRepository.save(Company.builder().name("Alpha Inc").owner(user).build());

        List<Company> result = companyRepository.findAllByOrderByNameAsc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Alpha Inc");
        assertThat(result.get(1).getName()).isEqualTo("Zeta Corp");
    }

    @Test
    void save_persistsAllFields() {
        Company company = Company.builder()
            .name("Test Corp").website("https://test.com").notes("note")
            .owner(user).build();

        Company saved = companyRepository.save(company);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Corp");
        assertThat(saved.getWebsite()).isEqualTo("https://test.com");
        assertThat(saved.getNotes()).isEqualTo("note");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
