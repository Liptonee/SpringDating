package org.petproject.dating_backend.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ access JWT токеном")
public record AuthResponseDto(
        @Schema(description = "access JWT",
                example = "dwdiANDoidaoida.dwAIDNaoFNgoain1r1.dWPOAGHPOmwdlkanwoa")
        String token
) {
}
