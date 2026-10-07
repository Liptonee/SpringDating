package org.petproject.dating_backend.chat;

public record ChatMessageEvent(

        Long messageId,

        Long senderId,

        Long receiverId,

        String content

) {
}
