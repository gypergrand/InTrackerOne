package com.intrackerone.domain.job;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "projectId", expression = "java(job.getProject() != null ? job.getProject().getId() : null)")
    @Mapping(target = "projectName", expression = "java(job.getProject() != null ? job.getProject().getName() : null)")
    @Mapping(target = "companyId", expression = "java(job.getProject() != null && job.getProject().getCompany() != null ? job.getProject().getCompany().getId() : null)")
    @Mapping(target = "companyName", expression = "java(job.getProject() != null && job.getProject().getCompany() != null ? job.getProject().getCompany().getName() : null)")
    @Mapping(target = "recruiterId", expression = "java(job.getRecruiter() != null ? job.getRecruiter().getId() : null)")
    @Mapping(target = "recruiterName", expression = "java(job.getRecruiter() != null ? job.getRecruiter().getName() : null)")
    @Mapping(target = "currentStatusId", expression = "java(job.getCurrentStatus() != null ? job.getCurrentStatus().getId() : null)")
    @Mapping(target = "currentStatusName", expression = "java(job.getCurrentStatus() != null ? job.getCurrentStatus().getName() : null)")
    @Mapping(target = "currentStatusColor", expression = "java(job.getCurrentStatus() != null ? job.getCurrentStatus().getColor() : null)")
    JobDto toDto(JobApplication job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "recruiter", ignore = true)
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobApplication toEntity(JobForm form);
}
