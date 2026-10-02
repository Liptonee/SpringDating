package org.petproject.dating_backend.swipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/swipes")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @Operation(summary = "Оперирует свайпом в системе",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = SwipeResponseDto.class)))
    @PostMapping()
    public ResponseEntity<SwipeResponseDto> swipe(
            @AuthenticationPrincipal Long curUserId,
            @RequestBody @Valid SwipeDto swipeDto
    ) {
        return ResponseEntity.ok(swipeService.swipe(curUserId, swipeDto));
    }

}
