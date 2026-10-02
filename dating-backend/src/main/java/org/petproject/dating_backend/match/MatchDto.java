package org.petproject.dating_backend.match;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Описывает мэтч")
public record MatchDto(

        @Schema(description = "ID самого мэтча", example = "2")
        Long id,

        @Schema(description = "ID первого участника мэтча", example = "63")
        Long firstUserId,

        @Schema(description = "ID второго участника мэтча", example = "567")
        Long secondUserId,

        @Schema(description = "Когда был совершён мэтч", example = "2026-09-25T10:15:30Z")
        Instant createdAt

) {
}
