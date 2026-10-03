package org.petproject.dating_backend.notification;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.swipe.LikeEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {


    private final NotificationSender notificationSender;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchCreated(MatchCreatedEvent event){
        notificationSender.send(event.firstUserId(),
                "Title for match created",
                "Congratulations! Body for match created");
        notificationSender.send(event.secondUserId(),
                "Title for match created",
                "Congratulations! Body for match created");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLike(LikeEvent event) {
        notificationSender.send(event.toId(),
                "Title for like",
                "User got a like");
    }



}
