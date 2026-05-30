package com.intrackerone.domain.history;

import com.intrackerone.domain.status.PipelineStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface StatusChangeMapper {

    @Mapping(target = "jobApplicationId", source = "jobApplication.id")
    @Mapping(target = "fromStatusId", source = "fromStatus", qualifiedByName = "extractId")
    @Mapping(target = "fromStatusName", source = "fromStatus", qualifiedByName = "extractName")
    @Mapping(target = "toStatusId", source = "toStatus.id")
    @Mapping(target = "toStatusName", source = "toStatus.name")
    StatusChangeDto toDto(StatusChange change);

    @Named("extractId")
    default Long extractId(PipelineStatus status) {
        return status != null ? status.getId() : null;
    }

    @Named("extractName")
    default String extractName(PipelineStatus status) {
        return status != null ? status.getName() : null;
    }
}
