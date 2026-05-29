package com.intrackerone.domain.compensation;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompensationEntryMapper {

    @Mapping(target = "jobApplicationId", source = "jobApplication.id")
    CompensationEntryDto toDto(CompensationEntry entry);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobApplication", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CompensationEntry toEntity(CompensationEntryForm form);
}
