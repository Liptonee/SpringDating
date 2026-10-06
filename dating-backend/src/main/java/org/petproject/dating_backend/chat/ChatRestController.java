package org.petproject.dating_backend.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Чаты", description = "История сообщений")
@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @Operation(summary = "История сообщений комнаты",
            description = "Cursor-based. Передайте beforeId=null для первой загрузки, " +
                    "затем beforeId = 'id самого старого загруженного сообщения' для подгрузки вверх." +
                    "Pageable с фиксированным размером в 50")
    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponseDto> getMessages(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeId
    ) {
        return chatService.getMessages(curUserId, roomId, beforeId);
    }
}