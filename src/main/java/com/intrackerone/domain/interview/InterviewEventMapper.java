package com.intrackerone.domain.interview;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InterviewEventMapper {

    @Mapping(target = "jobApplicationId", source = "jobApplication.id")
    InterviewEventDto toDto(InterviewEvent event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobApplication", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    InterviewEvent toEntity(InterviewEventForm form);
}
