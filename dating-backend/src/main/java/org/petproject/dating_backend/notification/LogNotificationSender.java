package org.petproject.dating_backend.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LogNotificationSender implements NotificationSender{

    @Override
    public void send(Long userId, String title, String body) {
        log.info("NOTIFICATION -> user {}: {} --- {}", userId, title, body);
    }
}
