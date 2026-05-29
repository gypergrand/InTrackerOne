package com.intrackerone.domain.project;

import lombok.Data;

@Data
public class ProjectForm {
    private String name;
    private String description;
    private Long companyId;
}
