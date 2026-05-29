package com.intrackerone.domain.interview;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewEventServiceTest {

    @Mock
    private InterviewEventRepository interviewEventRepository;

    @Mock
    private InterviewEventMapper interviewEventMapper;

    @Mock
    private JobApplicationRepository jobRepository;

    @InjectMocks
    private InterviewEventService interviewEventService;

    @Captor
    private ArgumentCaptor<InterviewEvent> eventCaptor;

    private User user;
    private JobApplication job;
    private InterviewEvent event;
    private InterviewEventDto eventDto;
    private InterviewEventForm form;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        job = JobApplication.builder().id(1L).position("Dev").owner(user).build();

        event = InterviewEvent.builder()
            .id(1L).jobApplication(job).title("Phone Screen")
            .eventDate(LocalDateTime.of(2025, 6, 1, 10, 0))
            .durationMinutes(45).format(EventFormat.VIDEO)
            .interviewerName("Jane").notes("Good").createdAt(LocalDateTime.now())
            .build();

        eventDto = new InterviewEventDto(1L, 1L, "Phone Screen",
            LocalDateTime.of(2025, 6, 1, 10, 0), 45, EventFormat.VIDEO,
            "Jane", "Good", LocalDateTime.now());

        form = new InterviewEventForm();
        form.setTitle("Phone Screen");
        form.setEventDate(LocalDateTime.of(2025, 6, 1, 10, 0));
        form.setDurationMinutes(45);
        form.setFormat(EventFormat.VIDEO);
        form.setInterviewerName("Jane");
        form.setNotes("Good");
    }

    @Test
    void findByJobId_returnsOrderedEvents() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(interviewEventRepository.findByJobApplicationIdOrderByEventDateAsc(1L))
            .thenReturn(List.of(event));
        when(interviewEventMapper.toDto(event)).thenReturn(eventDto);

        List<InterviewEventDto> result = interviewEventService.findByJobId(1L, 1L, Role.ADMIN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Phone Screen");
    }

    @Test
    void create_savesWithJobReference() {
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(interviewEventMapper.toEntity(form)).thenReturn(event);
        when(interviewEventRepository.save(any())).thenReturn(event);
        when(interviewEventMapper.toDto(event)).thenReturn(eventDto);

        InterviewEventDto result = interviewEventService.create(1L, form, 1L, Role.ADMIN);

        assertThat(result.title()).isEqualTo("Phone Screen");
        verify(interviewEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getJobApplication()).isEqualTo(job);
    }

    @Test
    void update_modifiesFields() {
        InterviewEventForm updateForm = new InterviewEventForm();
        updateForm.setTitle("Technical Interview");
        updateForm.setEventDate(LocalDateTime.of(2025, 6, 5, 14, 0));
        updateForm.setDurationMinutes(60);
        updateForm.setFormat(EventFormat.ONSITE);
        updateForm.setInterviewerName("Bob");
        updateForm.setNotes("In-person");

        when(interviewEventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(interviewEventRepository.save(any())).thenReturn(event);
        when(interviewEventMapper.toDto(event)).thenReturn(
            new InterviewEventDto(1L, 1L, "Technical Interview",
                LocalDateTime.of(2025, 6, 5, 14, 0), 60, EventFormat.ONSITE,
                "Bob", "In-person", LocalDateTime.now()));

        interviewEventService.update(1L, updateForm, 1L, Role.ADMIN);

        verify(interviewEventRepository).save(eventCaptor.capture());
        InterviewEvent saved = eventCaptor.getValue();
        assertThat(saved.getTitle()).isEqualTo("Technical Interview");
        assertThat(saved.getDurationMinutes()).isEqualTo(60);
        assertThat(saved.getFormat()).isEqualTo(EventFormat.ONSITE);
        assertThat(saved.getInterviewerName()).isEqualTo("Bob");
        assertThat(saved.getNotes()).isEqualTo("In-person");
    }

    @Test
    void delete_whenExists_deletes() {
        when(interviewEventRepository.findById(1L)).thenReturn(Optional.of(event));

        interviewEventService.delete(1L, 1L, Role.ADMIN);

        verify(interviewEventRepository).delete(event);
    }

    @Test
    void delete_whenNotFound_throwsException() {
        when(interviewEventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewEventService.delete(99L, 1L, Role.ADMIN))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
