package com.intrackerone.domain.project;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "industries", expression = "java(industryNames(project))")
    ProjectDto toDto(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "industries", ignore = true)
    Project toEntity(ProjectForm form);

    default Set<String> industryNames(Project project) {
        if (project.getIndustries() == null) {
            return Set.of();
        }
        return project.getIndustries().stream()
                .map(Industry::getName)
                .collect(Collectors.toSet());
    }
}
