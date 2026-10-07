package org.petproject.dating_backend.swipe;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = """
        Модель свайпа для истории свайпов.
        Используется в GET /swipes
        """)
public record SwipeHistoryDto(

        @Schema(description = "ID пользователя, которого свайпают",
                example = "213")
        Long toId,

        @Schema(description = "Лайк/Дизлайк",
                example = "LIKE")
        SwipeAction action,

        @Schema(description = "Когда был совершён свайп", example = "2026-09-25T10:15:30Z")
        Instant createdAt

) {
}