package org.petproject.dating_backend.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record MarkReadRequestDto(

        @Schema(description = "ID комнаты, в которой нужно прочитать все сообщения", example = "121")
        @NotNull
        Long roomId

) {
}
