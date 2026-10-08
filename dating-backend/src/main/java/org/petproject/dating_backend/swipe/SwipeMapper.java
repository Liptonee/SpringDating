package org.petproject.dating_backend.swipe;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SwipeMapper {

    SwipeHistoryDto toHistoryDto(SwipeEntity entity);

}
