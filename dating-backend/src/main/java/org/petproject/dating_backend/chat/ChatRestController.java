package org.petproject.dating_backend.chat;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.petproject.dating_backend.swipe.SwipeHistoryDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
                    "Pageable с фиксированным размером в 50",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{roomId}/messages")
    public List<ChatMessageResponseDto> getMessages(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeId
    ) {
        return chatService.getMessages(curUserId, roomId, beforeId);
    }


    @Operation(summary = "Выдаёт все команты-чаты пользователя.",
            description = "Имеется пагинация. Сортировка фиксирована createdAt DESC" +
                    "Возращает PageResponse<ChatRoomResponseDto>",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = ChatRoomResponseDto.class)))
    @GetMapping()
    public PageResponse<ChatRoomResponseDto> getRooms(
            @AuthenticationPrincipal Long curUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(chatService.getRooms(curUserId, pageable));
    }

}