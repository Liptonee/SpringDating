package org.petproject.dating_backend.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessageResponseDto sendMessage(
            @AuthenticationPrincipal Long curUserId,
            @Payload ChatMessageRequestDto request
    ) {
        return chatService.sendMessage(curUserId, request);
    }




}
