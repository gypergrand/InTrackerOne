package com.intrackerone.domain.history;

import java.time.LocalDateTime;

public record StatusChangeDto(
    Long id,
    Long jobApplicationId,
    Long fromStatusId,
    String fromStatusName,
    Long toStatusId,
    String toStatusName,
    LocalDateTime changedAt,
    String decidedBy,
    String notes,
    LocalDateTime createdAt
) {}
