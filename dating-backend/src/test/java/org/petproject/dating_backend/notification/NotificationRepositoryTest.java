package org.petproject.dating_backend.notification;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserGender;
import org.petproject.dating_backend.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class NotificationRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    private UserEntity persistUser(String email) {
        UserEntity u = new UserEntity();
        u.setEmail(email);
        u.setFirstName("User");
        u.setPasswordHash("hash");
        u.setReadyForDeck(true);
        u.setGender(UserGender.MALE);
        u.setAge((short) 25);
        u.setCity("Москва");
        u.setShortAbout("Кратко");
        u.setFullAbout("Полно");
        u.setPreferredAgeMin((short) 20);
        u.setPreferredAgeMax((short) 30);
        u.setRole(UserRole.USER);
        return testEntityManager.persistAndFlush(u);
    }

    private NotificationEntity persist(NotificationEntity notification) {
        return testEntityManager.persistAndFlush(notification);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.getEntityManager().clear();
    }

    private static NotificationEntity notification(Long userId,
                                                   String title,
                                                   boolean isRead,
                                                   Instant createdAt) {
        NotificationEntity n = new NotificationEntity();
        n.setUserId(userId);
        n.setTitle(title);
        n.setBody("Тело уведомления");
        n.setRead(isRead);
        n.setType(NotificationType.LIKE);
        n.setCreatedAt(createdAt);
        return n;
    }

    private static NotificationEntity notification(Long userId,
                                                   String title,
                                                   boolean isRead) {
        return notification(userId, title, isRead, Instant.now());
    }


    @Nested
    class FindAllByUserIdTest {

        @Test
        void returnsOnlyUserNotifications_whenReadIsNull() {
            Long firstId = persistUser("first@example.com").getId();
            Long secondId = persistUser("second@example.com").getId();

            NotificationEntity mineRead = persist(notification(firstId, "read", true));
            NotificationEntity mineUnread = persist(notification(firstId, "unread", false));
            persist(notification(secondId, "other-user", false));
            flushAndClear();

            Page<NotificationEntity> page = notificationRepository.findAllByUserId(
                    firstId, null, PageRequest.of(0, 10)
            );

            assertThat(page.getContent())
                    .extracting(NotificationEntity::getId)
                    .containsExactlyInAnyOrder(mineRead.getId(), mineUnread.getId());
        }

        @Test
        void filtersByReadFlag() {
            Long userId = persistUser("first@example.com").getId();

            NotificationEntity read = persist(notification(userId, "read", true));
            NotificationEntity unread = persist(notification(userId, "unread", false));
            flushAndClear();

            Page<NotificationEntity> readOnly = notificationRepository.findAllByUserId(
                    userId, true, PageRequest.of(0, 10)
            );
            Page<NotificationEntity> unreadOnly = notificationRepository.findAllByUserId(
                    userId, false, PageRequest.of(0, 10)
            );

            assertThat(readOnly.getContent())
                    .extracting(NotificationEntity::getId)
                    .containsExactly(read.getId());
            assertThat(unreadOnly.getContent())
                    .extracting(NotificationEntity::getId)
                    .containsExactly(unread.getId());
        }

        @Test
        void ordersByCreatedAtDesc() {
            Long userId = persistUser("first@example.com").getId();

            Instant t1 = Instant.parse("2024-01-01T10:00:00Z");
            Instant t2 = Instant.parse("2024-01-01T11:00:00Z");
            Instant t3 = Instant.parse("2024-01-01T12:00:00Z");

            NotificationEntity middle = persist(notification(userId, "middle", false, t2));
            NotificationEntity newest = persist(notification(userId, "newest", false, t3));
            NotificationEntity oldest = persist(notification(userId, "oldest", false, t1));
            flushAndClear();

            Page<NotificationEntity> page = notificationRepository.findAllByUserId(
                    userId, null, PageRequest.of(0, 10)
            );

            assertThat(page.getContent())
                    .extracting(NotificationEntity::getId)
                    .containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }

        @Test
        void appliesPageable() {
            Long userId = persistUser("first@example.com").getId();

            persist(notification(userId, "n1", false));
            persist(notification(userId, "n2", false));
            persist(notification(userId, "n3", false));
            flushAndClear();

            Page<NotificationEntity> page = notificationRepository.findAllByUserId(
                    userId, null, PageRequest.of(0, 2)
            );

            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(3);
        }

        @Test
        void returnsEmptyPage_whenUserHasNoNotifications() {
            Long emptyUserId = persistUser("empty@example.com").getId();
            Long otherUserId = persistUser("other@example.com").getId();

            persist(notification(otherUserId, "other", false));
            flushAndClear();

            Page<NotificationEntity> page = notificationRepository.findAllByUserId(
                    emptyUserId, null, PageRequest.of(0, 10)
            );

            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }


    @Nested
    class MarkAllAsReadTest {

        @Test
        void marksOnlyUnreadNotificationsOfGivenUserAsRead() {
            Long targetUserId = persistUser("target@example.com").getId();
            Long otherUserId = persistUser("other@example.com").getId();

            NotificationEntity targetRead = persist(notification(targetUserId, "target-read", true));
            NotificationEntity targetUnread1 = persist(notification(targetUserId, "target-unread-1", false));
            NotificationEntity targetUnread2 = persist(notification(targetUserId, "target-unread-2", false));
            NotificationEntity otherUnread = persist(notification(otherUserId, "other-unread", false));
            flushAndClear();

            notificationRepository.markAllAsRead(targetUserId);
            flushAndClear();

            List<NotificationEntity> user1 = notificationRepository.findAllByUserId(
                    targetUserId, null, PageRequest.of(0, 10)
            ).getContent();
            List<NotificationEntity> user2 = notificationRepository.findAllByUserId(
                    otherUserId, null, PageRequest.of(0, 10)
            ).getContent();

            assertThat(user1)
                    .extracting(NotificationEntity::getId)
                    .containsExactlyInAnyOrder(
                            targetRead.getId(), targetUnread1.getId(), targetUnread2.getId());
            assertThat(user1).allMatch(NotificationEntity::isRead);

            assertThat(user2)
                    .extracting(NotificationEntity::getId)
                    .containsExactly(otherUnread.getId());
            assertThat(user2.getFirst().isRead()).isFalse();
        }

        @Test
        void doesNotBreakAlreadyReadNotifications() {
            Long userId = persistUser("first@example.com").getId();

            NotificationEntity alreadyRead = persist(notification(userId, "already-read", true));
            flushAndClear();

            notificationRepository.markAllAsRead(userId);
            flushAndClear();

            NotificationEntity reloaded = testEntityManager.find(
                    NotificationEntity.class, alreadyRead.getId()
            );

            assertThat(reloaded).isNotNull();
            assertThat(reloaded.isRead()).isTrue();
        }
    }
}