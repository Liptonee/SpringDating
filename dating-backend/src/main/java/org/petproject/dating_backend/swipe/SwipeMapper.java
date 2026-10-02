package org.petproject.dating_backend.swipe;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SwipeMapper {

    SwipeHistoryDto toHistoryDto(SwipeEntity entity);

}
