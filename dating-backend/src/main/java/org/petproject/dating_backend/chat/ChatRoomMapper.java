package org.petproject.dating_backend.chat;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChatRoomMapper {

    default ChatRoomResponseDto toResponseDto(ChatRoomEntity entity, Long curUserId) {
        if (entity == null) return null;
        Long other = entity.getFirstUserId().equals(curUserId)
                ? entity.getSecondUserId()
                : entity.getFirstUserId();
        return new ChatRoomResponseDto(entity.getId(), other, entity.getCreatedAt());
    }
}