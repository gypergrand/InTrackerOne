package com.intrackerone.domain.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiTokenCreateForm {
    @NotBlank(message = "Token name is required")
    private String name;
}
