package com.intrackerone.domain.status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PipelineStatusForm {
    @NotBlank(message = "Status name is required")
    private String name;
    @NotBlank(message = "Color is required")
    private String color;
    @NotNull(message = "Status type is required")
    private StatusType type;
}
