package com.intrackerone.domain.job;

import com.intrackerone.domain.technology.Technology;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

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
    @Mapping(target = "ownerId", expression = "java(job.getOwner() != null ? job.getOwner().getId() : null)")
    @Mapping(target = "ownerName", expression = "java(job.getOwner() != null ? job.getOwner().getUsername() : null)")
    @Mapping(target = "technologies", source = "technologies", qualifiedByName = "technologyNames")
    JobDto toDto(JobApplication job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "recruiter", ignore = true)
    @Mapping(target = "currentStatus", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "technologies", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    JobApplication toEntity(JobForm form);

    @Named("technologyNames")
    default Set<String> technologyNames(Set<Technology> technologies) {
        if (technologies == null) return Set.of();
        return technologies.stream().map(Technology::getName).collect(Collectors.toSet());
    }
}
