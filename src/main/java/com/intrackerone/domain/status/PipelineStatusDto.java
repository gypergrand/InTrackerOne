package com.intrackerone.domain.status;

public record PipelineStatusDto(
    Long id,
    String name,
    int sortOrder,
    String color,
    StatusType type,
    boolean isDefault
) {}
