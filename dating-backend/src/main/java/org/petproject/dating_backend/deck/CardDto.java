package org.petproject.dating_backend.deck;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Основные данные о пользователе для отображения на карточке")
public record CardDto(

        @Schema(description = "ID пользователя", example = "512")
        Long userId,

        @Schema(description = "Ссылка на главную фотографию",
                example = "http://localhost:9000/photos/users/1/a1b2c3d4.jpg?X-Amz-Signature=...")
        String mainPhotoUrl,

        @Schema(description = "Имя пользователя",
                example = "Иван")
        String firstName,


        @Schema(description = "Краткое описание пользователя",
                example = "Краткое писание пользователя")
        String shortAbout,


        @Schema(description = "Возраст пользователя",
                example = "24")
        Short age

) {
}
