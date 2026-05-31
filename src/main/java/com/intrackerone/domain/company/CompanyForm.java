package com.intrackerone.domain.company;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyForm {
    @NotBlank(message = "Company name is required")
    private String name;
    private String website;
    private String notes;
}
