package org.petproject.dating_backend.notification;


import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PatchMapping("/{notifId}/read")
    public ResponseEntity<Void> read(
            @AuthenticationPrincipal Long curUserId,
            @PathVariable Long notifId
    ){
        notificationService.read(curUserId, notifId);
        return ResponseEntity.ok().build();
    }

    @GetMapping()
    public PageResponse<NotificationResponseDto> getNotifications(
            @AuthenticationPrincipal Long curUserId,
            @RequestParam(value = "read", required = false) Boolean read,
            @PageableDefault(size = 20) Pageable pageable
    ) {

        return PageResponse.from(notificationService.getNotifications(curUserId, read, pageable));

    }


}
