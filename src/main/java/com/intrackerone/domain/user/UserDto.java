package com.intrackerone.domain.user;

public record UserDto(
    Long id,
    String username,
    String email,
    boolean enabled,
    Role role
) {}
