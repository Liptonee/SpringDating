package org.petproject.dating_backend.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record MarkReadResponseDto(

        @Schema(description = "ID комнаты чата, к которой относится сообщение", example = "10")
        Long roomId,

        @Schema(description = "ID пользователя, который прочитал сообщение", example = "66")
        Long readerId,

        @Schema(description = "Время, когда сообщение было прочтено", example = "2025-01-15T10:30:00Z")
        Instant created_at

) {
}
