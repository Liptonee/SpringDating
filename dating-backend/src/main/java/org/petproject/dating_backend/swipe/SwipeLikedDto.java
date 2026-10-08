package org.petproject.dating_backend.swipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = """
        Модель свайпа для выборки тех, кто лайкнул пользователя.
        """)
public record SwipeLikedDto(

        @Schema(description = "ID пользователя, который лайкнул",
                example = "213")
        Long fromId,

        @Schema(description = "Когда был совершён свайп", example = "2026-09-25T10:15:30Z")
        Instant createdAt

) {
}