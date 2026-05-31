package com.intrackerone.domain.recruiter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class RecruiterForm {
    @NotBlank(message = "Recruiter name is required")
    private String name;
    private String email;
    private String phone;
    private String notes;
    @NotNull(message = "Company is required")
    private Long companyId;
    private Set<Long> projectIds = new HashSet<>();
}
