package org.petproject.dating_backend.photo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Фото пользователя")
public record PhotoDto(
        @Schema(description = "ID фото", example = "42")
        Long id,

        @Schema(description = "ID владельца", example = "1")
        Long userId,

        @Schema(description = "Исходное имя файла", example = "cat.jpg")
        String originalName,

        @Schema(description = "MIME-тип файла", example = "image/jpeg")
        String contentType,

        @Schema(description = "Размер файла в байтах", example = "245123")
        Long size,

        @Schema(description = "Само фото в виде временной ссылки",
                example = "http://localhost:9000/photos/users/1/a1b2c3d4.jpg?X-Amz-Signature=...")
        String url,

        @Schema(description = "Когда фото было загружено", example = "2026-09-25T10:15:30Z")
        Instant uploadedAt
) {
}
