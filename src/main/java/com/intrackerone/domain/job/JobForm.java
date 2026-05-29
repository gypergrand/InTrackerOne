package com.intrackerone.domain.job;

import lombok.Data;

@Data
public class JobForm {
    private String position;
    private String jobDescription;
    private String jobUrl;
    private String location;
    private String salaryRange;
    private JobType jobType;
    private Source source;
    private String sourceMeta;
    private String notes;
    private Long projectId;
    private Long recruiterId;
    private Long currentStatusId;
}
