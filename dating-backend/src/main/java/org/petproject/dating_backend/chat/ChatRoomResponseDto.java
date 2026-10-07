package org.petproject.dating_backend.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Данные чат-комнаты")
public record ChatRoomResponseDto(

        @Schema(description = "ID чат-комнаты", example = "42")
        Long roomId,

        @Schema(description = "ID собеседника (второго участника чата)", example = "7")
        Long secondOneUserId,

        @Schema(description = "Дата и время создания чат-комнаты", example = "2025-01-15T10:30:00Z")
        Instant createdAt

) {
}