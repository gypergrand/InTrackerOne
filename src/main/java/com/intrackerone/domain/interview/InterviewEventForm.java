package com.intrackerone.domain.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewEventForm {
    @NotNull(message = "Event date is required")
    private LocalDateTime eventDate;
    @NotBlank(message = "Title is required")
    private String title;
    private Integer durationMinutes;
    @NotNull(message = "Format is required")
    private EventFormat format;
    private String interviewerName;
    private String notes;
}
