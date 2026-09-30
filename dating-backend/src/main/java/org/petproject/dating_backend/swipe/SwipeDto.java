package org.petproject.dating_backend.swipe;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = """
        Описывает действие - свайп.
        Используется в POST /swipes
        """)
public record SwipeDto(
        @Schema(description = "ID пользователя, которого свайпают",
                example = "213")
        @NotNull
        Long toId,

        @NotNull
        @Schema(description = "Лайк/Дизлайк",
                example = "LIKE")
        SwipeAction action
) {
}
