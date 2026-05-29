package com.intrackerone.domain.job;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "companyId", source = "project.company.id")
    @Mapping(target = "companyName", source = "project.company.name")
    @Mapping(target = "recruiterId", source = "recruiter.id")
    @Mapping(target = "recruiterName", source = "recruiter.name")
    @Mapping(target = "currentStatusId", source = "currentStatus.id")
    @Mapping(target = "currentStatusName", source = "currentStatus.name")
    @Mapping(target = "currentStatusColor", source = "currentStatus.color")
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
