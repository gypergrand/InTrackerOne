package com.intrackerone.domain.compensation;

import com.intrackerone.domain.job.JobApplication;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.job.JobType;
import com.intrackerone.domain.job.Source;
import com.intrackerone.domain.status.PipelineStatus;
import com.intrackerone.domain.status.StatusType;
import com.intrackerone.domain.user.Role;
import com.intrackerone.domain.user.User;
import com.intrackerone.infrastructure.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompensationEntryServiceTest {

    @Mock
    private CompensationEntryRepository compensationEntryRepository;

    @Mock
    private CompensationEntryMapper compensationEntryMapper;

    @Mock
    private JobApplicationRepository jobRepository;

    @InjectMocks
    private CompensationEntryService compensationEntryService;

    @Captor
    private ArgumentCaptor<CompensationEntry> entryCaptor;

    private User user;
    private JobApplication job;
    private CompensationEntry entry;
    private CompensationEntryDto entryDto;
    private CompensationEntryForm form;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();

        job = JobApplication.builder().id(1L).position("Dev").owner(user).build();

        entry = CompensationEntry.builder()
            .id(1L).jobApplication(job)
            .rateType(RateType.ANNUAL_GROSS).amount(new BigDecimal("100000"))
            .currency(Currency.EUR).category(CompCategory.OFFER)
            .notes("Great offer").createdAt(LocalDateTime.now())
            .build();

        entryDto = new CompensationEntryDto(1L, 1L, RateType.ANNUAL_GROSS,
            new BigDecimal("100000"), Currency.EUR, CompCategory.OFFER,
            "Great offer", LocalDateTime.now());

        form = new CompensationEntryForm();
        form.setRateType(RateType.ANNUAL_GROSS);
        form.setAmount(new BigDecimal("100000"));
        form.setCurrency(Currency.EUR);
        form.setCategory(CompCategory.OFFER);
        form.setNotes("Great offer");
    }

    @Test
    void findByJobId_returnsEntries() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(compensationEntryRepository.findByJobApplicationId(1L)).thenReturn(List.of(entry));
        when(compensationEntryMapper.toDto(entry)).thenReturn(entryDto);

        List<CompensationEntryDto> result = compensationEntryService.findByJobId(1L, 1L, Role.ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    void create_savesWithJobReference() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(compensationEntryMapper.toEntity(form)).thenReturn(entry);
        when(compensationEntryRepository.save(any())).thenReturn(entry);
        when(compensationEntryMapper.toDto(entry)).thenReturn(entryDto);

        CompensationEntryDto result = compensationEntryService.create(1L, form, 1L, Role.ADMIN);

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("100000"));
        verify(compensationEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getJobApplication()).isEqualTo(job);
    }

    @Test
    void update_modifiesFields() {
        CompensationEntryForm updateForm = new CompensationEntryForm();
        updateForm.setRateType(RateType.MONTHLY);
        updateForm.setAmount(new BigDecimal("8000"));
        updateForm.setCurrency(Currency.USD);
        updateForm.setCategory(CompCategory.POSTED);
        updateForm.setNotes("Updated");

        when(compensationEntryRepository.findById(1L)).thenReturn(Optional.of(entry));
        when(compensationEntryRepository.save(any())).thenReturn(entry);
        when(compensationEntryMapper.toDto(entry)).thenReturn(
            new CompensationEntryDto(1L, 1L, RateType.MONTHLY, new BigDecimal("8000"),
                Currency.USD, CompCategory.POSTED, "Updated", LocalDateTime.now()));

        compensationEntryService.update(1L, updateForm, 1L, Role.ADMIN);

        verify(compensationEntryRepository).save(entryCaptor.capture());
        CompensationEntry saved = entryCaptor.getValue();
        assertThat(saved.getRateType()).isEqualTo(RateType.MONTHLY);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("8000"));
        assertThat(saved.getCurrency()).isEqualTo(Currency.USD);
        assertThat(saved.getCategory()).isEqualTo(CompCategory.POSTED);
        assertThat(saved.getNotes()).isEqualTo("Updated");
    }

    @Test
    void delete_whenExists_deletes() {
        when(compensationEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        compensationEntryService.delete(1L, 1L, Role.ADMIN);

        verify(compensationEntryRepository).delete(entry);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(compensationEntryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compensationEntryService.delete(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
