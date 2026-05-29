package com.intrackerone.domain.history;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StatusChangeMapper {

    @Mapping(target = "jobApplicationId", source = "jobApplication.id")
    @Mapping(target = "fromStatusId", source = "fromStatus.id")
    @Mapping(target = "fromStatusName", source = "fromStatus.name")
    @Mapping(target = "toStatusId", source = "toStatus.id")
    @Mapping(target = "toStatusName", source = "toStatus.name")
    StatusChangeDto toDto(StatusChange change);
}
