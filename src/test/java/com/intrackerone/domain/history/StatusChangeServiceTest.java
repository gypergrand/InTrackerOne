package com.intrackerone.domain.history;

import com.intrackerone.domain.job.JobApplication;
import com.intrackerone.domain.job.JobApplicationRepository;
import com.intrackerone.domain.status.PipelineStatus;
import com.intrackerone.domain.status.PipelineStatusRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatusChangeServiceTest {

    @Mock
    private StatusChangeRepository statusChangeRepository;

    @Mock
    private StatusChangeMapper statusChangeMapper;

    @Mock
    private JobApplicationRepository jobRepository;

    @Mock
    private PipelineStatusRepository pipelineStatusRepository;

    @InjectMocks
    private StatusChangeService statusChangeService;

    @Captor
    private ArgumentCaptor<StatusChange> changeCaptor;

    private User user;
    private PipelineStatus fromStatus;
    private PipelineStatus toStatus;
    private JobApplication job;
    private StatusChange change;
    private StatusChangeDto changeDto;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();

        fromStatus = PipelineStatus.builder()
            .id(1L).name("Applied").sortOrder(1).color("#2196F3")
            .type(StatusType.ACTIVE).owner(user).build();

        toStatus = PipelineStatus.builder()
            .id(2L).name("Phone Screen").sortOrder(2).color("#00BCD4")
            .type(StatusType.ACTIVE).owner(user).build();

        job = JobApplication.builder().id(1L).position("Dev").currentStatus(fromStatus).owner(user).build();

        change = StatusChange.builder()
            .id(1L).jobApplication(job).fromStatus(fromStatus).toStatus(toStatus)
            .changedAt(LocalDateTime.now()).decidedBy("COMPANY").notes("Moving forward")
            .build();

        changeDto = new StatusChangeDto(1L, 1L, 1L, "Applied", 2L, "Phone Screen",
            LocalDateTime.now(), "COMPANY", "Moving forward", LocalDateTime.now());
    }

    @Test
    void findByJobId_returnsOrderedHistory() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(statusChangeRepository.findByJobApplicationIdOrderByChangedAtDesc(1L))
            .thenReturn(List.of(change));
        when(statusChangeMapper.toDto(change)).thenReturn(changeDto);

        List<StatusChangeDto> result = statusChangeService.findByJobId(1L, 1L, Role.ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).toStatusName()).isEqualTo("Phone Screen");
    }

    @Test
    void recordChange_updatesJobStatusAndCreatesHistory() {
        when(jobRepository.findByIdWithLock(1L)).thenReturn(Optional.of(job));
        when(pipelineStatusRepository.findById(2L)).thenReturn(Optional.of(toStatus));
        when(jobRepository.saveAndFlush(any())).thenReturn(job);
        when(statusChangeRepository.saveAndFlush(any())).thenReturn(change);
        when(statusChangeMapper.toDto(change)).thenReturn(changeDto);

        StatusChangeDto result = statusChangeService.recordChange(1L, 2L, "COMPANY", "Moving forward", 1L, Role.ADMIN);

        assertThat(result.toStatusName()).isEqualTo("Phone Screen");

        verify(jobRepository).saveAndFlush(any());
        assertThat(job.getCurrentStatus()).isEqualTo(toStatus);

        verify(statusChangeRepository).saveAndFlush(changeCaptor.capture());
        StatusChange saved = changeCaptor.getValue();
        assertThat(saved.getFromStatus()).isEqualTo(fromStatus);
        assertThat(saved.getToStatus()).isEqualTo(toStatus);
        assertThat(saved.getDecidedBy()).isEqualTo("COMPANY");
    }

    @Test
    void recordChange_whenJobNotFound_throwsException() {
        when(jobRepository.findByIdWithLock(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusChangeService.recordChange(99L, 1L, null, null, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findRecentByOwnerId_returnsLimitedResults() {
        when(statusChangeRepository.findByJobApplicationOwnerIdOrderByChangedAtDesc(1L))
            .thenReturn(List.of(change, change));
        when(statusChangeMapper.toDto(any())).thenReturn(changeDto);

        List<StatusChangeDto> result = statusChangeService.findRecentByOwnerId(1L, 1);

        assertThat(result).hasSize(1);
    }
}
