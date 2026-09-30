package org.petproject.dating_backend.deck;

import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/deck")
@RequiredArgsConstructor
public class DeckController {


    private final DeckService deckService;


    @GetMapping()
    ResponseEntity<List<CardDto>> getDeck(
            @AuthenticationPrincipal Long curUserId,
            @RequestParam(value = "quantity", required = false) Short quantity
    ) throws MinioException {
        quantity = quantity == null ? 5 : quantity;
        return ResponseEntity.ok(deckService.getDeck(curUserId, quantity));
    }


}
