package org.petproject.dating_backend.chat;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ChatRoomMapper {

    default ChatRoomResponseDto toResponseDto(ChatRoomEntity entity, Long curUserId) {
        if (entity == null) return null;
        Long other = entity.getFirstUserId().equals(curUserId)
                ? entity.getSecondUserId()
                : entity.getFirstUserId();
        return new ChatRoomResponseDto(entity.getId(), other, entity.getCreatedAt());
    }
}