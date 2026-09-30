package org.petproject.dating_backend.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = """
        Все данные о пользователе.
        Используется для PATCH (PATCH /users/me) методов в качестве указания того, что нужно изменить,
        в данном случае id указывать нельзя.
        Также используется как ответ GET метода (GET /users/me) для получения профиля текущего пользователя,
        в данном случае id присутствует.
        """)
public record ProfileUserDto(

        @Schema(description = "ID пользователя. При запросе обязательно null, при ответе присутствует",
                example = "213")
        @Null
        Long id,


        @Schema(description = "Имя пользователя (от 2 до 30 символов)",
                example = "Иван")
        @Size(max = 30, min = 2)
        String firstName,


        @Schema(description = "Email пользователя (максимум 50 символов)",
                example = "example@mail.com")
        @Email
        @Size(max = 50)
        String email,


        @Schema(description = "Краткое описание пользователя (от 0 до 127 символов)",
                example = "Краткое писание пользователя")
        @Size(max = 127)
        String shortAbout,


        @Schema(description = "Полное описание пользователя (от 0 до 511 символов)",
                example = "Полное писание пользователя")
        @Size(max = 512)
        String fullAbout,


        @Schema(description = "Возраст пользователя (от 18 до 100)",
                example = "24")
        @Max(100)
        @Min(18)
        Short age,


        @Schema(description = "ID главного фото, оно отображается на карточке",
                example = "17")
        Long mainPhotoId,


        @Schema(description = "Город пользователя (от 2 до 40 символов)",
                example = "Москва")
        @Size(max = 40, min = 2)
        String city,


        @Schema(description = "Пол пользователя",
                example = "FEMALE")
        UserGender gender,


        @Schema(description = "Нижняя граница диапазона поиска по возрасту",
                example = "20")
        @Max(100)
        @Min(18)
        Short preferredAgeMin,


        @Schema(description = "Верхняя граница диапазона поиска по возрасту",
                example = "59")
        @Max(100)
        @Min(18)
        Short preferredAgeMax,

        @Schema(description = "Готов ли пользователь к получению колоды/выдаче в колоде",
                example = "true")
        Boolean readyForDeck


) {
}
