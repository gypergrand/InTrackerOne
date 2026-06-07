package com.intrackerone.domain.project;

import com.intrackerone.domain.technology.Technology;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyName", source = "company.name")
    @Mapping(target = "industries", expression = "java(industryNames(project))")
    @Mapping(target = "technologies", source = "technologies", qualifiedByName = "technologyNames")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.username")
    ProjectDto toDto(Project project);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "industries", ignore = true)
    @Mapping(target = "technologies", ignore = true)
    Project toEntity(ProjectForm form);

    default Set<String> industryNames(Project project) {
        if (project.getIndustries() == null) {
            return Set.of();
        }
        return project.getIndustries().stream()
                .map(Industry::getName)
                .collect(Collectors.toSet());
    }

    @Named("technologyNames")
    default Set<String> technologyNames(Set<Technology> technologies) {
        if (technologies == null) {
            return Set.of();
        }
        return technologies.stream()
                .map(Technology::getName)
                .collect(Collectors.toSet());
    }
}
