package org.petproject.dating_backend.notification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long USER_ID = 123L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long NOTIF_ID = 42L;
    private static final Instant CREATED_AT = Instant.parse("2026-10-03T12:34:56Z");

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.counter(anyString())).thenReturn(counter);
        lenient().when(registry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }


    private static NotificationEntity notification(Long id, Long userId, boolean read, NotificationType type) {
        NotificationEntity e = new NotificationEntity();
        e.setId(id);
        e.setUserId(userId);
        e.setTitle("Новый матч!");
        e.setBody("Вы понравились друг другу с Анной");
        e.setRead(read);
        e.setType(type);
        e.setCreatedAt(CREATED_AT);
        return e;
    }

    private static NotificationResponseDto toDto(NotificationEntity e) {
        return new NotificationResponseDto(
                e.getId(), e.getTitle(), e.getBody(), e.getType(),
                e.isRead(), e.getCreatedAt());
    }


    @Nested
    class CreateTest {

        @Test
        void savesEntityAndIncrementsCounterWithTypeTag() {
            NotificationRequestDto request = new NotificationRequestDto(
                    USER_ID, "Новый матч!", "Вы понравились друг другу с Анной", NotificationType.MATCH);

            notificationService.create(request);

            ArgumentCaptor<NotificationEntity> captor = ArgumentCaptor.forClass(NotificationEntity.class);
            verify(notificationRepository).save(captor.capture());
            NotificationEntity saved = captor.getValue();

            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getTitle()).isEqualTo("Новый матч!");
            assertThat(saved.getBody()).isEqualTo("Вы понравились друг другу с Анной");
            assertThat(saved.getType()).isEqualTo(NotificationType.MATCH);
            assertThat(saved.isRead()).isFalse();

            verify(registry).counter("notifications.created.total", "type", "MATCH");
            verify(counter).increment();
        }
    }


    @Nested
    class ReadTest {

        @Test
        void throwsNotFound_whenNotificationDoesNotExist() {
            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.read(USER_ID, NOTIF_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Уведомления с таким id не существует");

            verify(notificationRepository, never()).save(any());
            verify(counter, never()).increment();
        }

        @Test
        void throwsForbidden_whenNotificationBelongsToAnotherUser() {
            NotificationEntity existing = notification(NOTIF_ID, OTHER_USER_ID, false, NotificationType.LIKE);
            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> notificationService.read(USER_ID, NOTIF_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Вы не можете прочитать чужое уведомление");

            assertThat(existing.isRead()).isFalse();
            verify(notificationRepository, never()).save(any());
            verify(counter, never()).increment();
        }

        @Test
        void marksAsReadAndSaves_whenUnread() {
            NotificationEntity existing = notification(NOTIF_ID, USER_ID, false, NotificationType.CHAT_MESSAGE);
            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.of(existing));

            notificationService.read(USER_ID, NOTIF_ID);

            assertThat(existing.isRead()).isTrue();
            verify(notificationRepository).save(existing);
            verify(registry).counter("notifications.read.total");
            verify(counter).increment();
        }

        @Test
        void doesNotSaveNorIncrement_whenAlreadyRead() {
            NotificationEntity existing = notification(NOTIF_ID, USER_ID, true, NotificationType.LIKE);
            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.of(existing));

            notificationService.read(USER_ID, NOTIF_ID);

            assertThat(existing.isRead()).isTrue();
            verify(notificationRepository, never()).save(any());
            verify(counter, never()).increment();
        }
    }


    @Nested
    class GetSingleNotificationTest {

        @Test
        void returnsDto_whenOwner() {
            NotificationEntity entity = notification(NOTIF_ID, USER_ID, false, NotificationType.LIKE);
            NotificationResponseDto dto = toDto(entity);

            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.of(entity));
            when(notificationMapper.toResponse(entity)).thenReturn(dto);

            NotificationResponseDto result = notificationService.getSingleNotification(USER_ID, NOTIF_ID);

            assertThat(result).isEqualTo(dto);
            verify(notificationMapper).toResponse(entity);
        }

        @Test
        void throwsNotFound_whenNotificationDoesNotExist() {
            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.getSingleNotification(USER_ID, NOTIF_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Уведомления с таким id не существует");

            verifyNoInteractions(notificationMapper);
        }

        @Test
        void throwsForbidden_whenNotificationBelongsToAnotherUser() {
            NotificationEntity entity = notification(NOTIF_ID, OTHER_USER_ID, false, NotificationType.MATCH);
            when(notificationRepository.findById(NOTIF_ID)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> notificationService.getSingleNotification(USER_ID, NOTIF_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Вы не можете просмотреть чужое уведомление");

            verifyNoInteractions(notificationMapper);
        }
    }


    @Nested
    class GetNotificationsTest {

        @Test
        void returnsMappedPage() {
            Pageable pageable = PageRequest.of(0, 20);
            NotificationEntity entity = notification(NOTIF_ID, USER_ID, false, NotificationType.LIKE);
            NotificationResponseDto dto = toDto(entity);

            when(notificationRepository.findAllByUserId(USER_ID, true, pageable))
                    .thenReturn(new PageImpl<>(List.of(entity)));
            when(notificationMapper.toResponse(entity)).thenReturn(dto);

            Page<NotificationResponseDto> result =
                    notificationService.getNotifications(USER_ID, true, pageable);

            assertThat(result.getContent()).containsExactly(dto);
            verify(notificationRepository).findAllByUserId(USER_ID, true, pageable);
        }

        @Test
        void passesNullReadFilter_whenNotProvided() {
            Pageable pageable = PageRequest.of(0, 20);

            when(notificationRepository.findAllByUserId(USER_ID, null, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<NotificationResponseDto> result =
                    notificationService.getNotifications(USER_ID, null, pageable);

            assertThat(result).isEmpty();
            verify(notificationRepository).findAllByUserId(USER_ID, null, pageable);
            verifyNoInteractions(notificationMapper);
        }
    }


    @Nested
    class MarkAllAsReadTest {

        @Test
        void incrementsCounterAndMarksAllAsRead() {
            notificationService.markAllAsRead(USER_ID);

            verify(registry).counter("notifications.read_all.total");
            verify(counter).increment();
            verify(notificationRepository).markAllAsRead(USER_ID);
        }
    }
}