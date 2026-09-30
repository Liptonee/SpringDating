package org.petproject.dating_backend.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Используется для получения основных данных о другом (не текущем) пользователе.
        """)
public record GetUserDto(

        @Schema(description = "ID пользователя. При запросе обязательно null, при ответе присутствует",
                example = "213")
        Long id,


        @Schema(description = "Имя пользователя (от 2 до 30 символов)",
                example = "Иван")
        String firstName,


        @Schema(description = "Краткое описание пользователя (от 0 до 127 символов)",
                example = "Краткое писание пользователя")
        String shortAbout,


        @Schema(description = "Полное описание пользователя (от 0 до 511 символов)",
                example = "Полное писание пользователя")
        String fullAbout,


        @Schema(description = "Возраст пользователя (от 18 до 100)",
                example = "24")
        Integer age,


        @Schema(description = "ID главного фото, оно отображается на карточке",
                example = "17")
        String mainPhotoId,


        @Schema(description = "Город пользователя (от 2 до 40 символов)",
                example = "Москва")
        String city,


        @Schema(description = "Пол пользователя",
                example = "FEMALE")
        UserGender gender
) {

}
