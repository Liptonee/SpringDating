package org.petproject.dating_backend.swipe;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Описывает действие - свайп.
        Используется в POST /swipes
        """)
public record SwipeDto(

        @Schema(description = "ID пользователя, который делает свайп",
                example = "213")
        Long fromId,

        @Schema(description = "ID пользователя, которого свайпают",
                example = "213")
        Long toId,

        @Schema(description = "Лайк/Дизлайк",
                example = "LIKE")
        SwipeAction action
) {
}
