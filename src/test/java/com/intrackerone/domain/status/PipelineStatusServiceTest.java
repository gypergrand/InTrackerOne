package com.intrackerone.domain.status;

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

    @InjectMocks
    private PipelineStatusService pipelineStatusService;

    @Captor
    private ArgumentCaptor<PipelineStatus> statusCaptor;

    private User user;
    private PipelineStatus activeStatus;
    private PipelineStatusDto statusDto;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("user").build();

        activeStatus = PipelineStatus.builder()
            .id(1L).name("Applied").sortOrder(1).color("#2196F3")
            .type(StatusType.ACTIVE).owner(user).build();

        statusDto = new PipelineStatusDto(1L, "Applied", 1, "#2196F3", StatusType.ACTIVE);
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
        when(pipelineStatusRepository.existsByOwnerIdAndName(1L, "Preparation")).thenReturn(true);

        pipelineStatusService.seedDefaultsForUser(1L);

        verify(pipelineStatusRepository, never()).saveAll(any());
    }

    @Test
    void seedDefaultsForUser_whenNotSeeded_createsAllDefaults() {
        when(pipelineStatusRepository.existsByOwnerIdAndName(1L, "Preparation")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        pipelineStatusService.seedDefaultsForUser(1L);

        verify(pipelineStatusRepository).saveAll(any());
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
            new PipelineStatusDto(2L, "Custom Stage", 2, "#FF0000", StatusType.ACTIVE));

        PipelineStatusDto result = pipelineStatusService.create(form, 1L);

        assertThat(result.name()).isEqualTo("Custom Stage");
        assertThat(result.sortOrder()).isEqualTo(2);

        verify(pipelineStatusRepository).save(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getOwner()).isEqualTo(user);
    }

    @Test
    void findById_whenNotFound_throwsException() {
        when(pipelineStatusRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pipelineStatusService.findById(99L, 1L, com.intrackerone.domain.user.Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
