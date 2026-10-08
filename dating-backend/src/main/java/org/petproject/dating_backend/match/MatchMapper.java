package org.petproject.dating_backend.match;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MatchMapper {

    MatchDto toDto(MatchEntity entity);

}
