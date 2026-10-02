package org.petproject.dating_backend.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "Выдаёт все мэтчи пользователя",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Успех",
            content = @Content(schema = @Schema(implementation = MatchDto.class)))
    @GetMapping
    public PageResponse<MatchDto> getMatches(
            @AuthenticationPrincipal Long curUserId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(matchService.getMatches(curUserId, pageable));
    }

}
