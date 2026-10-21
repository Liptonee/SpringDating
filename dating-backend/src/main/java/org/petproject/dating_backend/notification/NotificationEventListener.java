package org.petproject.dating_backend.notification;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.chat.ChatMessageEvent;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.swipe.LikeEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Async("notificationExecutor")
public class NotificationEventListener {


    private final NotificationSender sender;
    private final NotificationService service;

    private final ObjectProvider<NotificationEventListener> selfProvider;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchCreated(MatchCreatedEvent event) {
        String title = "Title for match created";
        String body = "Congratulations! Body for match created. ";

        NotificationRequestDto firstRequestDto = new NotificationRequestDto(
                event.firstUserId(), title, body, NotificationType.MATCH
        );

        NotificationRequestDto secondRequestDto = new NotificationRequestDto(
                event.secondUserId(), title, body, NotificationType.MATCH
        );

        selfProvider.getObject().createBoth(firstRequestDto, secondRequestDto);

        sender.send(event.firstUserId(),
                title,
                body + "User1=" + event.firstUserId() + "User2=" + event.secondUserId());
        sender.send(event.secondUserId(),
                title,
                body + "User1=" + event.secondUserId() + "User2=" + event.firstUserId());

    }

    @Transactional
    public void createBoth(NotificationRequestDto first, NotificationRequestDto second) {
        service.create(first);
        service.create(second);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLike(LikeEvent event) {
        String title = "Title for like";
        String body = "User got a like from user=" + event.fromId();

        NotificationRequestDto requestDto = new NotificationRequestDto(
                event.toId(), title, body, NotificationType.LIKE
        );

        service.create(requestDto);

        sender.send(event.toId(), title, body);

    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatMessage(ChatMessageEvent event) {
        String title = "Title for message";
        String body = String.format(
                "Body for message. Room id: %d, Sender id: %d, Receiver id: %d, content %s",
                event.roomId(),
                event.senderId(),
                event.receiverId(),
                event.content());

        NotificationRequestDto requestDto = new NotificationRequestDto(
                event.receiverId(), title, body, NotificationType.CHAT_MESSAGE
        );

        service.create(requestDto);

        sender.send(event.receiverId(), title, body);


    }

}
