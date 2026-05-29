package com.intrackerone.domain.interview;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewEventForm {
    private LocalDateTime eventDate;
    private String title;
    private Integer durationMinutes;
    private EventFormat format;
    private String interviewerName;
    private String notes;
}
