package com.intrackerone.domain.status;

public record PipelineStatusDto(
    Long id,
    String name,
    String color,
    StatusType type
) {}
