package org.petproject.dating_backend.notification;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {

    NotificationResponseDto toResponse(NotificationEntity entity);

}
