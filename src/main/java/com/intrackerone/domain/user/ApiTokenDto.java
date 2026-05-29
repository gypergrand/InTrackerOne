package com.intrackerone.domain.user;

import java.time.LocalDateTime;

public record ApiTokenDto(
    Long id,
    String name,
    boolean enabled,
    LocalDateTime lastUsedAt,
    LocalDateTime createdAt
) {}
