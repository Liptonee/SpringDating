package org.petproject.dating_backend.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Описывает возникшие исключения и ошибки")
public record ErrorDto(

        @Schema(description = "HTTP стату код ошибки",
                example = "404")
        Integer statusCode,

        @Schema(description = "Краткая информация про произошло не так",
                example = "Conflict data")
        String title,

        @Schema(description = "Более подробная информация",
                example = "Такой email уже используется")
        String details
) {
}
