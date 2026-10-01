package org.petproject.dating_backend.swipe;

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

    @PostMapping()
    public ResponseEntity<SwipeResponseDto> swipe(
            @AuthenticationPrincipal Long curUserId,
            @RequestBody @Valid SwipeDto swipeDto
    ){
        return ResponseEntity.ok(swipeService.swipe(curUserId, swipeDto));
    }

}
