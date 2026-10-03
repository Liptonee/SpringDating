package org.petproject.dating_backend.notification;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.swipe.LikeEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {


    private final NotificationSender sender;
    private final NotificationService service;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchCreated(MatchCreatedEvent event){

        String title = "Title for match created";
        String body = "Congratulations! Body for match created. ";

        sender.send(event.firstUserId(),
                title,
                body + "User1=" + event.firstUserId() + "User2=" + event.secondUserId());
        sender.send(event.secondUserId(),
                title,
                body + "User1=" + event.secondUserId() + "User2=" + event.firstUserId());


        NotificationRequestDto firstRequestDto = new NotificationRequestDto(
                event.firstUserId(), title, body, NotificationType.MATCH
        );

        NotificationRequestDto secondRequestDto = new NotificationRequestDto(
                event.secondUserId(), title, body, NotificationType.MATCH
        );

        service.create(firstRequestDto);
        service.create(secondRequestDto);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLike(LikeEvent event) {
        String title = "Title for like";
        String body = "User got a like from user=" + event.fromId();

        sender.send(event.toId(), title, body);

        NotificationRequestDto requestDto = new NotificationRequestDto(
                event.toId(), title, body, NotificationType.LIKE
        );

        service.create(requestDto);

    }




}
