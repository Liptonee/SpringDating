package org.petproject.dating_backend.match;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @GetMapping
    public PageResponse<MatchDto> getMatches(
            @AuthenticationPrincipal Long curUserId,
            @PageableDefault(size = 20) Pageable pageable
    ){
        return PageResponse.from(matchService.getMatches(curUserId, pageable));
    }

}
