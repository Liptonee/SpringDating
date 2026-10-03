package org.petproject.dating_backend.notification;

public record NotificationRequestDto(
        Long userId,
        String title,
        String body,
        NotificationType type
) {
}
