package com.intrackerone.domain.status;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PipelineStatusMapper {

    PipelineStatusDto toDto(PipelineStatus status);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sortOrder", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PipelineStatus toEntity(PipelineStatusForm form);
}
