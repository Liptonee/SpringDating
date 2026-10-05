package org.petproject.dating_backend.notification;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseDto toResponse(NotificationEntity entity);

}
