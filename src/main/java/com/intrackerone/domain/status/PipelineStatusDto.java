package com.intrackerone.domain.status;

public record PipelineStatusDto(
    Long id,
    String name,
    Integer sortOrder,
    String color,
    StatusType type
) {}
