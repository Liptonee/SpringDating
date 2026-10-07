package org.petproject.dating_backend.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на сообщение")
public record ChatMessageRequestDto(

        @Schema(description = "Сообщение, которое отправляет пользователь в чат", example = "Привет!")
        @NotBlank @Size(max = 2000) String content

) {
}
