package com.intrackerone.domain.job;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobDto(
    Long id,
    String position,
    String jobDescription,
    String jobUrl,
    String location,
    String salaryRange,
    JobType jobType,
    WorkType workType,
    OnCallSchedule onCallSchedule,
    String onCallDetails,
    JobTimezone timezone,
    String timezoneOther,
    LocalDate firstAppeared,
    LocalDate repostedAt,
    Source source,
    String sourceMeta,
    String notes,
    Long projectId,
    String projectName,
    Long companyId,
    String companyName,
    Long recruiterId,
    String recruiterName,
    Long currentStatusId,
    String currentStatusName,
    String currentStatusColor,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
