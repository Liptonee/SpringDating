package org.petproject.dating_backend.notification;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Уведомление пользователя")
public record NotificationDto(

        @Schema(description = "ID уведомления", example = "42")
        Long id,

        @Schema(description = "Заголовок", example = "Новый матч!")
        String title,

        @Schema(description = "Текст уведомления",
                example = "Вы понравились друг другу с Анной")
        String body,

        @Schema(description = "Тип уведомления", example = "MATCH")
        NotificationType type,

        @Schema(description = "Прочитано ли", example = "false")
        boolean isRead,

        @Schema(description = "Когда создано", example = "2026-10-03T12:34:56Z")
        Instant createdAt
) {}