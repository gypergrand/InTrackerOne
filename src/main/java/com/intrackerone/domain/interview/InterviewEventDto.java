package com.intrackerone.domain.interview;

import java.time.LocalDateTime;

public record InterviewEventDto(
    Long id,
    Long jobApplicationId,
    String title,
    LocalDateTime eventDate,
    Integer durationMinutes,
    EventFormat format,
    String interviewerName,
    String notes,
    LocalDateTime createdAt
) {}
