package org.petproject.dating_backend.chat;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChatMessageMapper {


    @Mapping(target = "senderName", expression = "java(senderNames.get(entity.getSenderId()))")
    @Mapping(target = "isRead", expression = "java(entity.isRead())")
    ChatMessageResponseDto toDto(ChatMessageEntity entity, Map<Long, String> senderNames);
}