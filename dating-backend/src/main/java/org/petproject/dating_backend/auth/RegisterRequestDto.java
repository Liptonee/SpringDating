package org.petproject.dating_backend.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.petproject.dating_backend.user.UserGender;


@Schema(description = "Запрос на регистрацию в систему")
public record RegisterRequestDto(

        @Schema(description = "Email пользователя (максимум 50 символов)",
                example = "ivan@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Email
        @NotNull
        @Size(max = 50)
        String email,


        @Schema(description = "Реальное имя пользователя (то 2 до 30 символов)",
                example = "Иван",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 30, min = 2)
        String firstName,


        @Schema(description = "Пароль (не менее 6 символов)",
                example = "SecurePass123!",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Size(min = 6)
        String password
) {
}
