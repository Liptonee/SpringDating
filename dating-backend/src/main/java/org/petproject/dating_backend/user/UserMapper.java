package org.petproject.dating_backend.user;


import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(UserEntity entity);

}
