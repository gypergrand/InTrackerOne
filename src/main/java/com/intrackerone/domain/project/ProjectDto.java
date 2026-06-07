package com.intrackerone.domain.project;

import java.util.Set;

public record ProjectDto(
    Long id,
    String name,
    String description,
    Long companyId,
    String companyName,
    Set<String> industries,
    Set<String> technologies,
    Long ownerId,
    String ownerName
) {}
