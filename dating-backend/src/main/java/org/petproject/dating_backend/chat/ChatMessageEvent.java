package org.petproject.dating_backend.chat;

public record ChatMessageEvent(

        Long messageId,

        Long roomId,

        Long senderId,

        Long receiverId,

        String content

) {
}
