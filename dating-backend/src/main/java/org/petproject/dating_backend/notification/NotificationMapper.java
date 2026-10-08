package org.petproject.dating_backend.notification;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface NotificationMapper {

    @Mapping(target = "isRead", expression = "java(entity.isRead())")
    NotificationResponseDto toResponse(NotificationEntity entity);

}
