package org.petproject.dating_backend.user;


import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface UserMapper {

    ProfileUserDto toProfileDto(UserEntity entity);

    GetUserDto toGetDto(UserEntity entity);



}
