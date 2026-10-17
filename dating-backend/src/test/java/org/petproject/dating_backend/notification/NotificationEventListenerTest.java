package org.petproject.dating_backend.notification;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.chat.ChatMessageEvent;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.swipe.LikeEvent;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    private static final Long MATCH_ID = 50L;
    private static final Long SWIPE_ID = 60L;
    private static final Long MESSAGE_ID = 70L;
    private static final Long ROOM_ID = 80L;
    private static final Long USER_A = 1L;
    private static final Long USER_B = 2L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-03T12:34:56Z");

    @Mock
    private NotificationSender sender;
    @Mock
    private NotificationService service;
    @Mock
    private ObjectProvider<NotificationEventListener> selfProvider;

    @InjectMocks
    private NotificationEventListener listener;


    private NotificationEventListener mockSelf() {
        NotificationEventListener self = mock(NotificationEventListener.class);
        when(selfProvider.getObject()).thenReturn(self);
        return self;
    }


    @Nested
    class OnMatchCreatedTest {

        @Test
        void createsBothNotificationsAndSendsToBothUsers() {
            NotificationEventListener self = mockSelf();
            MatchCreatedEvent event = new MatchCreatedEvent(
                    MATCH_ID, USER_A, USER_B, CREATED_AT);

            listener.onMatchCreated(event);

            ArgumentCaptor<NotificationRequestDto> dtoCaptor =
                    ArgumentCaptor.forClass(NotificationRequestDto.class);
            verify(self).createBoth(dtoCaptor.capture(), dtoCaptor.capture());
            List<NotificationRequestDto> dtos = dtoCaptor.getAllValues();

            assertThat(dtos).hasSize(2);
            assertThat(dtos.get(0).userId()).isEqualTo(USER_A);
            assertThat(dtos.get(0).type()).isEqualTo(NotificationType.MATCH);
            assertThat(dtos.get(1).userId()).isEqualTo(USER_B);
            assertThat(dtos.get(1).type()).isEqualTo(NotificationType.MATCH);

            String title = "Title for match created";
            String baseBody = "Congratulations! Body for match created. ";

            verify(sender).send(USER_A, title,
                    eq(baseBody + "User1=" + USER_A + "User2=" + USER_B));
            verify(sender).send(USER_B, title,
                    eq(baseBody + "User1=" + USER_B + "User2=" + USER_A));
        }
    }


    @Nested
    class CreateBothTest {

        @Test
        void createsTwoNotifications() {
            NotificationRequestDto first = new NotificationRequestDto(
                    USER_A, "t1", "b1", NotificationType.MATCH);
            NotificationRequestDto second = new NotificationRequestDto(
                    USER_B, "t2", "b2", NotificationType.MATCH);

            listener.createBoth(first, second);

            verify(service).create(first);
            verify(service).create(second);
        }
    }


    @Nested
    class OnLikeTest {

        @Test
        void createsNotificationAndSendsToReceiver() {
            LikeEvent event = new LikeEvent(SWIPE_ID, USER_A, USER_B, CREATED_AT);

            listener.onLike(event);

            ArgumentCaptor<NotificationRequestDto> captor =
                    ArgumentCaptor.forClass(NotificationRequestDto.class);
            verify(service).create(captor.capture());
            NotificationRequestDto dto = captor.getValue();

            assertThat(dto.userId()).isEqualTo(USER_B);
            assertThat(dto.type()).isEqualTo(NotificationType.LIKE);
            assertThat(dto.body()).isEqualTo("User got a like from user=" + USER_A);

            verify(sender).send(USER_B, "Title for like",
                    "User got a like from user=" + USER_A);
        }
    }


    @Nested
    class OnChatMessageTest {

        @Test
        void createsNotificationAndSendsToReceiver() {
            ChatMessageEvent event = new ChatMessageEvent(
                    MESSAGE_ID, ROOM_ID, USER_A, USER_B, "Hello!");

            listener.onChatMessage(event);

            ArgumentCaptor<NotificationRequestDto> captor =
                    ArgumentCaptor.forClass(NotificationRequestDto.class);
            verify(service).create(captor.capture());
            NotificationRequestDto dto = captor.getValue();

            String expectedBody = String.format(
                    "Body for message. Room id: %d, Sender id: %d, Receiver id: %d, content %s",
                    ROOM_ID, USER_A, USER_B, "Hello!");

            assertThat(dto.userId()).isEqualTo(USER_B);
            assertThat(dto.type()).isEqualTo(NotificationType.CHAT_MESSAGE);
            assertThat(dto.body()).isEqualTo(expectedBody);

            verify(sender).send(USER_B, "Title for message", expectedBody);
        }
    }
}