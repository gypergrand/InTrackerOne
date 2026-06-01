package com.intrackerone.domain.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobForm {
    @NotBlank(message = "Position is required")
    private String position;
    private String jobDescription;
    private String jobUrl;
    private String location;
    private String salaryRange;
    @NotNull(message = "Job type is required")
    private JobType jobType;
    @NotNull(message = "Work type is required")
    private WorkType workType;
    @NotNull(message = "Source is required")
    private Source source;
    private String sourceMeta;
    private String notes;
    @NotNull(message = "Project is required")
    private Long projectId;
    private Long recruiterId;
    @NotNull(message = "Status is required")
    private Long currentStatusId;
}
