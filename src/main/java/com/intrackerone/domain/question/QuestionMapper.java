package com.intrackerone.domain.question;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    @Mapping(target = "recruiterId", source = "recruiter.id")
    @Mapping(target = "recruiterName", source = "recruiter.name")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.username")
    QuestionDto toDto(Question question);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recruiter", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Question toEntity(QuestionForm form);
}
