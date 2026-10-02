package org.petproject.dating_backend.swipe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @Operation(summary = "Оперирует свайпом в системе",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = SwipeResponseDto.class)))
    @PostMapping("/swipes")
    public ResponseEntity<SwipeResponseDto> swipe(
            @AuthenticationPrincipal Long curUserId,
            @RequestBody @Valid SwipeRequestDto swipeRequestDto
    ) {
        return ResponseEntity.ok(swipeService.swipe(curUserId, swipeRequestDto));
    }

    @Operation(summary = "Выдаёт историю свайпов." +
            "Имеется фильтрация, пагинация. Сортировка фиксирована createdAt DESC",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = PageResponse.class)))
    @GetMapping("/swipes/history")
    public PageResponse<SwipeHistoryDto> getHistory(
            @AuthenticationPrincipal Long curUserId,
            @RequestParam(required = false) SwipeAction action,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(swipeService.getHistory(curUserId, action, pageable));
    }




}
