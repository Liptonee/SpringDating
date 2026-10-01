package org.petproject.dating_backend.swipe;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Описания результата свайпа")
public record SwipeResponseDto (

        @Schema(description = "Определяет случился ли мэтч", example = "true")
        boolean isMatched
) {
}
