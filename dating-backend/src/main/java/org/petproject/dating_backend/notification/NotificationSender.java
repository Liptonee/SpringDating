package org.petproject.dating_backend.notification;

public interface NotificationSender {

    void send(Long userId, String title, String body);

}
