package org.petproject.dating_backend.photo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PhotoMapper {

    @Mapping(source = "entity.user.id", target = "userId")
    @Mapping(source = "url", target = "url")
    PhotoDto toDto(PhotoEntity entity, String url);

}
