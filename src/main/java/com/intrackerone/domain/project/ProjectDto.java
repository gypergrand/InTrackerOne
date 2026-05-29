package com.intrackerone.domain.project;

public record ProjectDto(
    Long id,
    String name,
    String description,
    Long companyId,
    String companyName
) {}
