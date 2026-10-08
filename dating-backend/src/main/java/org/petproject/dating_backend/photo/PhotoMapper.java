package org.petproject.dating_backend.photo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PhotoMapper {

    @Mapping(source = "url", target = "url")
    PhotoDto toDto(PhotoEntity entity, String url);

}
