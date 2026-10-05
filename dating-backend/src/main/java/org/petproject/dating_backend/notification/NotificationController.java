package org.petproject.dating_backend.notification;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.petproject.dating_backend.swipe.SwipeHistoryDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    @Operation(summary = "Отмечает уведомление прочитанным.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "Уведомление успешно прочитано")
    @PatchMapping("/{notifId}/read")
    public ResponseEntity<Void> read(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long notifId
    ){
        notificationService.read(curUserId, notifId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Возвращает одно уведомление по id",
            description = "Возвращает NotificationResponseDto. Доступно только владельцу уведомления.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = NotificationResponseDto.class))
    )
    @GetMapping("/{notifId}")
    public NotificationResponseDto getSingleNotification(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long notifId
    ) {
        return notificationService.getSingleNotification(curUserId, notifId);
    }

    @Operation(summary = "Выдаёт все уведомления",
            description = "Имеется фильтрация по непрочитанным, пагинация. Сортировка фиксирована createdAt DESC" +
                    "Возращает PageResponse<SwipeHistoryDto>",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = SwipeHistoryDto.class)))
    @GetMapping()
    public PageResponse<NotificationResponseDto> getNotifications(
            @AuthenticationPrincipal Long curUserId,
            @RequestParam(value = "read", required = false) Boolean read,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return PageResponse.from(notificationService.getNotifications(curUserId, read, pageable));

    }

    @Operation(summary = "Отметить все уведомления как прочитанные")
    @ApiResponse(responseCode = "204", description = "Все уведомления успешно прочитаны")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Long curUserId) {
        notificationService.markAllAsRead(curUserId);
        return ResponseEntity.noContent().build();
    }


}
