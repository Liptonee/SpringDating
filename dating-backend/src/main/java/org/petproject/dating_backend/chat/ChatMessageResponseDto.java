package org.petproject.dating_backend.chat;


import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "DTO ответа с данными сообщения чата")
public record ChatMessageResponseDto(

        @Schema(description = "ID сообщения", example = "111")
        Long id,

        @Schema(description = "ID комнаты чата, к которой относится сообщение", example = "10")
        Long roomId,

        @Schema(description = "ID отправителя сообщения", example = "42")
        Long senderId,

        @Schema(description = "Отображаемое имя отправителя", example = "John Doe")
        String senderName,

        @Schema(description = "Текстовое содержимое сообщения", example = "Привет, как дела?")
        String content,

        @Schema(description = "Дата и время создания сообщения в формате ISO-8601", example = "2025-01-15T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Флаг, указывающий, прочитано ли сообщение", example = "false")
        boolean isRead
) {}
