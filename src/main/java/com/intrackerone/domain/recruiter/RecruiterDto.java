package com.intrackerone.domain.recruiter;

import java.util.Set;

public record RecruiterDto(
    Long id,
    String name,
    String email,
    String phone,
    String notes,
    Long companyId,
    String companyName,
    Set<Long> projectIds,
    Long ownerId,
    String ownerName
) {}
