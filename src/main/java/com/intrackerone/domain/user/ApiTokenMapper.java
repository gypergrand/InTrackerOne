package com.intrackerone.domain.user;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ApiTokenMapper {

    ApiTokenDto toDto(ApiToken token);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tokenHash", ignore = true)
    @Mapping(target = "tokenIdentifier", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ApiToken toEntity(ApiTokenCreateForm form);
}
