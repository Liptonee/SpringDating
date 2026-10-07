package org.petproject.dating_backend.chat;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.petproject.dating_backend.user.UserService;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mappings({
            @Mapping(target = "id",         source = "messageEntity.id"),
            @Mapping(target = "roomId",     source = "messageEntity.roomId"),
            @Mapping(target = "senderId",   source = "messageEntity.senderId"),
            @Mapping(target = "senderName", expression = "java(userService.getFirstName(messageEntity.getSenderId()))"),
            @Mapping(target = "content",    source = "messageEntity.content"),
            @Mapping(target = "createdAt",  source = "messageEntity.createdAt"),
            @Mapping(target = "isRead",       source = "messageEntity.read")
    })
    ChatMessageResponseDto toDto(ChatMessageEntity messageEntity, UserService userService);
}