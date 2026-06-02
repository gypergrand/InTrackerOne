package com.intrackerone.domain.status;

import com.intrackerone.domain.history.StatusChangeRepository;
import com.intrackerone.domain.job.JobApplicationRepository;
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
class PipelineStatusServiceTest {

    @Mock
    private PipelineStatusRepository pipelineStatusRepository;

    @Mock
    private PipelineStatusMapper pipelineStatusMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private StatusChangeRepository statusChangeRepository;

    @InjectMocks
    private PipelineStatusService pipelineStatusService;

    @Captor
    private ArgumentCaptor<PipelineStatus> statusCaptor;

    private User user;
    private PipelineStatus activeStatus;
    private PipelineStatusDto statusDto;
    private PipelineStatus defaultStatus;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user").build();

        activeStatus = PipelineStatus.builder()
            .id(1L).name("Applied").sortOrder(1).color("#2196F3")
            .type(StatusType.ACTIVE).isDefault(false).owner(user).build();

        defaultStatus = PipelineStatus.builder()
            .id(10L).name("Preparation").sortOrder(1).color("#9E9E9E")
            .type(StatusType.ACTIVE).isDefault(true).owner(null).build();

        statusDto = new PipelineStatusDto(1L, "Applied", 1, "#2196F3", StatusType.ACTIVE, false);
    }

    @Test
    void findByOwnerId_returnsOrderedStatuses() {
        when(pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(1L))
            .thenReturn(List.of(activeStatus));
        when(pipelineStatusMapper.toDto(activeStatus)).thenReturn(statusDto);

        List<PipelineStatusDto> result = pipelineStatusService.findByOwnerId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Applied");
    }

    @Test
    void seedDefaultsForUser_whenAlreadySeeded_skips() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(1L))
            .thenReturn(List.of(activeStatus));

        pipelineStatusService.seedDefaultsForUser(1L);

        verify(pipelineStatusRepository, never()).saveAll(any());
    }

    @Test
    void seedDefaultsForUser_whenNotSeeded_createsAllDefaults() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(1L))
            .thenReturn(List.of());
        when(pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc())
            .thenReturn(List.of(defaultStatus));

        pipelineStatusService.seedDefaultsForUser(1L);

        verify(pipelineStatusRepository).saveAll(any());
    }

    @Test
    void findDefaultStatuses_returnsDefaultStatuses() {
        PipelineStatusDto defaultDto = new PipelineStatusDto(10L, "Preparation", 1, "#9E9E9E", StatusType.ACTIVE, true);
        when(pipelineStatusRepository.findByIsDefaultTrueOrderBySortOrderAsc())
            .thenReturn(List.of(defaultStatus));
        when(pipelineStatusMapper.toDto(defaultStatus)).thenReturn(defaultDto);

        List<PipelineStatusDto> result = pipelineStatusService.findDefaultStatuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Preparation");
        assertThat(result.get(0).isDefault()).isTrue();
    }

    @Test
    void create_assignsNextSortOrder() {
        PipelineStatusForm form = new PipelineStatusForm();
        form.setName("Custom Stage");
        form.setColor("#FF0000");
        form.setType(StatusType.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(pipelineStatusRepository.findByOwnerIdOrderBySortOrderAsc(1L))
            .thenReturn(List.of(activeStatus));
        when(pipelineStatusMapper.toEntity(form)).thenReturn(
            PipelineStatus.builder().name("Custom Stage").build());
        when(pipelineStatusRepository.save(any())).thenReturn(
            PipelineStatus.builder().id(2L).name("Custom Stage").sortOrder(2).build());
        when(pipelineStatusMapper.toDto(any())).thenReturn(
            new PipelineStatusDto(2L, "Custom Stage", 2, "#FF0000", StatusType.ACTIVE, false));

        PipelineStatusDto result = pipelineStatusService.create(form, 1L);

        assertThat(result.name()).isEqualTo("Custom Stage");

        verify(pipelineStatusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getOwner()).isEqualTo(user);
    }

    @Test
    void findById_whenNotFound_throwsException() {
        when(pipelineStatusRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineStatusService.findById(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
