package com.intrackerone.domain.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class ProjectForm {
    @NotBlank(message = "Project name is required")
    private String name;
    private String description;
    @NotNull(message = "Company is required")
    private Long companyId;
    private Set<String> industries = new HashSet<>();
}
