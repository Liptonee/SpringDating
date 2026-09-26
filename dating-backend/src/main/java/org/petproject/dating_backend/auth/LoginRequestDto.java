package org.petproject.dating_backend.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на вход в систему")
public record LoginRequestDto(

        @Schema(description = "Email пользователя",
                example = "ivan@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Email
        @NotNull
        String email,

        @Schema(description = "Пароль (не менее 6 символов)",
                example = "SecurePass123!",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Size(min = 6)
        String password
) {
}
