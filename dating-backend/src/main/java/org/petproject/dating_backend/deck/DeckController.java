package org.petproject.dating_backend.deck;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/deck")
@RequiredArgsConstructor
@Tag(name = "Колода", description = "Формирование колоды карточек для свайпа")
public class DeckController {


    private final DeckService deckService;

    @Operation(summary = "Возвращает список карточек пользователей",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CardDto.class))))
    @GetMapping()
    ResponseEntity<List<CardDto>> getDeck(
            @AuthenticationPrincipal Long curUserId,
            @RequestParam(value = "quantity", required = false) Short quantity
    ) {
        quantity = quantity == null ? 5 : quantity;
        return ResponseEntity.ok(deckService.getDeck(curUserId, quantity));
    }


}
