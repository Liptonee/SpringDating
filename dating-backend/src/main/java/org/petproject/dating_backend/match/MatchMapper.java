package org.petproject.dating_backend.match;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    MatchDto toDto(MatchEntity entity);

}
