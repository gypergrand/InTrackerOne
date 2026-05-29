package com.intrackerone.domain.status;

import lombok.Data;

@Data
public class PipelineStatusForm {
    private String name;
    private String color;
    private StatusType type;
}
