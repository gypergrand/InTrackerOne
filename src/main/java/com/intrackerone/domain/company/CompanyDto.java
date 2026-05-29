package com.intrackerone.domain.company;

public record CompanyDto(
    Long id,
    String name,
    String website,
    String notes
) {}
