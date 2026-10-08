package org.petproject.dating_backend.user;


import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

    ProfileUserDto toProfileDto(UserEntity entity);

    GetUserDto toGetDto(UserEntity entity);


}
