package org.petproject.dating_backend.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.common.exception.ErrorDto;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessageResponseDto sendMessage(
            @AuthenticationPrincipal Long curUserId,
            @Payload @Valid ChatMessageRequestDto request
    ) {
        return chatService.sendMessage(curUserId, request);
    }


    @MessageMapping("/chat.markRead")
    @SendTo("/topic/chat/room/{roomId}/read")
    public MarkReadResponseDto markRead(
            @Payload @Valid MarkReadRequestDto request,
            @AuthenticationPrincipal Long curUserId
    ) {
        chatService.markRead(curUserId, request.roomId());
        return new MarkReadResponseDto(request.roomId(), curUserId, Instant.now());
    }


    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorDto handleException(Exception e) {
        log.warn("WebSocket error", e);
        return new ErrorDto(500, "Chat error", e.getMessage());
    }



}
