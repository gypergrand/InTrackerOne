package com.intrackerone.domain.recruiter;

import com.intrackerone.domain.project.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecruiterMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "projectIds", source = "projects", qualifiedByName = "projectToIds")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.username")
    RecruiterDto toDto(Recruiter recruiter);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Recruiter toEntity(RecruiterForm form);

    @Named("projectToIds")
    default Set<Long> projectToIds(Set<Project> projects) {
        if (projects == null) return Set.of();
        return projects.stream().map(Project::getId).collect(Collectors.toSet());
    }
}
