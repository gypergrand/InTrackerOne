package com.intrackerone.domain.company;

import com.intrackerone.domain.status.PipelineStatusService;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.domain.user.UserRepository;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PipelineStatusService pipelineStatusService;

    @InjectMocks
    private CompanyService companyService;

    @Captor
    private ArgumentCaptor<Company> companyCaptor;

    private User admin;
    private User normalUser;
    private Company company;
    private CompanyDto companyDto;
    private CompanyForm form;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        normalUser = User.builder().id(2L).username("user").role(Role.USER).build();

        company = Company.builder()
            .id(1L).name("TestCorp").website("https://test.com").notes("notes")
            .owner(admin).build();

        companyDto = new CompanyDto(1L, "TestCorp", "https://test.com", "notes", 1L, "admin");

        form = new CompanyForm();
        form.setName("TestCorp");
        form.setWebsite("https://test.com");
        form.setNotes("notes");
    }

    @Test
    void findAll_forAdmin_returnsAllCompanies() {
        when(companyRepository.findAllByOrderByNameAsc()).thenReturn(List.of(company));
        when(companyMapper.toDto(company)).thenReturn(companyDto);

        List<CompanyDto> result = companyService.findAll(1L, Role.ADMIN);

        assertThat(result).hasSize(1);
        verify(companyRepository, never()).findByOwnerId(any());
    }

    @Test
    void findAll_forUser_returnsOwnCompanies() {
        when(companyRepository.findByOwnerId(2L)).thenReturn(List.of(company));
        when(companyMapper.toDto(company)).thenReturn(companyDto);

        List<CompanyDto> result = companyService.findAll(2L, Role.USER);

        assertThat(result).hasSize(1);
    }

    @Test
    void create_savesWithOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(companyMapper.toEntity(form)).thenReturn(Company.builder().name("TestCorp").build());
        when(companyRepository.save(any())).thenReturn(company);
        when(companyMapper.toDto(company)).thenReturn(companyDto);

        CompanyDto result = companyService.create(form, 1L);

        assertThat(result.name()).isEqualTo("TestCorp");
        verify(companyRepository).save(companyCaptor.capture());
        assertThat(companyCaptor.getValue().getOwner()).isEqualTo(admin);
    }

    @Test
    void findById_whenExists_returnsCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyMapper.toDto(company)).thenReturn(companyDto);

        CompanyDto result = companyService.findById(1L, 1L, Role.ADMIN);

        assertThat(result.name()).isEqualTo("TestCorp");
    }

    @Test
    void findById_whenNotFound_throwsException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.findById(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_modifiesFields() {
        CompanyForm updateForm = new CompanyForm();
        updateForm.setName("NewName");
        updateForm.setWebsite("https://new.com");
        updateForm.setNotes("new notes");

        when(companyRepository.findById(1L)).thenReturn(Optional.of(company));
        when(companyRepository.save(any())).thenReturn(company);
        when(companyMapper.toDto(company)).thenReturn(companyDto);

        companyService.update(1L, updateForm, 1L, Role.ADMIN);

        verify(companyRepository).save(companyCaptor.capture());
        Company saved = companyCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("NewName");
        assertThat(saved.getWebsite()).isEqualTo("https://new.com");
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.delete(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
