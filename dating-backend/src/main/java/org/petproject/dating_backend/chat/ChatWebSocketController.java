package org.petproject.dating_backend.chat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.common.exception.ErrorDto;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/room/{roomId}/sendMessage")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessageResponseDto sendMessage(
            @AuthenticationPrincipal Long curUserId,
            @DestinationVariable Long roomId,
            @Payload @Valid ChatMessageRequestDto request
    ) {
        return chatService.sendMessage(curUserId, roomId, request);
    }


    @MessageMapping("/chat/room/{roomId}/markRead")
    @SendTo("/topic/chat/room/{roomId}/read")
    public MarkReadResponseDto markRead(
            @AuthenticationPrincipal Long curUserId,
            @DestinationVariable Long roomId
    ) {
        chatService.markRead(curUserId, roomId);
        return new MarkReadResponseDto(roomId, curUserId, Instant.now());
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public ErrorDto handleException(Exception e) {
        log.warn("WebSocket error", e);
        return new ErrorDto(500, "Chat error", e.getMessage());
    }



}
