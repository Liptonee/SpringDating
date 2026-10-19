package org.petproject.dating_backend.chat;

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
class ChatMessageRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    private <T> T persist(T entity) {
        return testEntityManager.persistAndFlush(entity);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.getEntityManager().clear();
    }

    private static UserEntity user(String email, String firstName) {
        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setEmail(email);
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
        return u;
    }

    private ChatRoomEntity newRoom(UserEntity a, UserEntity b) {
        return chatRoomRepository.createBetween(a.getId(), b.getId());
    }

    private static ChatMessageEntity message(Long roomId, Long senderId, String content) {
        ChatMessageEntity m = new ChatMessageEntity();
        m.setRoomId(roomId);
        m.setSenderId(senderId);
        m.setContent(content);
        m.setCreatedAt(Instant.now());
        return m;
    }


    @Nested
    class MarkAllReadInRoomTest {

        @Test
        void marksOnlyUnreadMessagesFromOtherSenders() {
            UserEntity reader = persist(user("reader@example.com", "Reader"));
            UserEntity other = persist(user("other@example.com", "Other"));
            ChatRoomEntity room = persist(newRoom(reader, other));

            ChatMessageEntity unreadFromOther =
                    persist(message(room.getId(), other.getId(), "unread from other"));
            ChatMessageEntity unreadFromReader =
                    persist(message(room.getId(), reader.getId(), "unread from reader"));
            ChatMessageEntity alreadyRead =
                    message(room.getId(), other.getId(), "already read");
            alreadyRead.setRead(true);
            persist(alreadyRead);
            flushAndClear();

            chatMessageRepository.markAllReadInRoom(room.getId(), reader.getId());
            flushAndClear();

            assertThat(testEntityManager
                    .find(ChatMessageEntity.class, unreadFromOther.getId())
                    .isRead()).isTrue();
            assertThat(testEntityManager
                    .find(ChatMessageEntity.class, unreadFromReader.getId())
                    .isRead()).isFalse();
            assertThat(testEntityManager
                    .find(ChatMessageEntity.class, alreadyRead.getId())
                    .isRead()).isTrue();
        }

        @Test
        void doesNotAffectOtherRooms() {
            UserEntity reader = persist(user("reader@example.com", "Reader"));
            UserEntity other = persist(user("other@example.com", "Other"));
            UserEntity third = persist(user("third@example.com", "Third"));

            ChatRoomEntity targetRoom = persist(newRoom(reader, other));
            ChatRoomEntity otherRoom = persist(newRoom(reader, third));

            ChatMessageEntity inTargetRoom =
                    persist(message(targetRoom.getId(), other.getId(), "target"));
            ChatMessageEntity inOtherRoom =
                    persist(message(otherRoom.getId(), third.getId(), "other"));
            flushAndClear();

            chatMessageRepository.markAllReadInRoom(targetRoom.getId(), reader.getId());
            flushAndClear();

            assertThat(testEntityManager
                    .find(ChatMessageEntity.class, inTargetRoom.getId())
                    .isRead()).isTrue();
            assertThat(testEntityManager
                    .find(ChatMessageEntity.class, inOtherRoom.getId())
                    .isRead()).isFalse();
        }
    }


    @Nested
    class FindPageTest {

        @Test
        void returnsMessagesForRoomOrderedByIdDesc() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            UserEntity c = persist(user("c@example.com", "C"));
            ChatRoomEntity targetRoom = persist(newRoom(a, b));
            ChatRoomEntity otherRoom = persist(newRoom(a, c));

            ChatMessageEntity m1 =
                    persist(message(targetRoom.getId(), a.getId(), "first"));
            persist(message(otherRoom.getId(), c.getId(), "other room"));
            ChatMessageEntity m3 =
                    persist(message(targetRoom.getId(), b.getId(), "second"));
            flushAndClear();

            List<ChatMessageEntity> result = chatMessageRepository.findPage(
                    targetRoom.getId(),
                    null,
                    PageRequest.of(0, 10)
            );

            assertThat(result)
                    .extracting(ChatMessageEntity::getId)
                    .containsExactly(m3.getId(), m1.getId());
        }

        @Test
        void filtersByBeforeId() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            ChatRoomEntity room = persist(newRoom(a, b));

            ChatMessageEntity m1 = persist(message(room.getId(), a.getId(), "first"));
            ChatMessageEntity m2 = persist(message(room.getId(), b.getId(), "second"));
            persist(message(room.getId(), a.getId(), "third"));
            flushAndClear();

            List<ChatMessageEntity> result = chatMessageRepository.findPage(
                    room.getId(),
                    m2.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(result)
                    .extracting(ChatMessageEntity::getId)
                    .containsExactly(m1.getId());
        }

        @Test
        void appliesPageable() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            ChatRoomEntity room = persist(newRoom(a, b));

            persist(message(room.getId(), a.getId(), "first"));
            persist(message(room.getId(), b.getId(), "second"));
            persist(message(room.getId(), a.getId(), "third"));
            flushAndClear();

            List<ChatMessageEntity> result = chatMessageRepository.findPage(
                    room.getId(),
                    null,
                    PageRequest.of(0, 2)
            );

            assertThat(result).hasSize(2);
        }
    }
}