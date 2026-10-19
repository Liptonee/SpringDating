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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ChatRoomRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

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

    private ChatRoomEntity newRoom(Long a, Long b) {
        return chatRoomRepository.createBetween(a, b);
    }


    @Nested
    class HasMemberByIdTest {

        @Test
        void returnsTrue_whenUserIsMember() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            ChatRoomEntity room = persist(newRoom(a.getId(), b.getId()));
            flushAndClear();

            assertThat(chatRoomRepository.hasMemberById(room.getId(), a.getId())).isTrue();
            assertThat(chatRoomRepository.hasMemberById(room.getId(), b.getId())).isTrue();
        }

        @Test
        void returnsFalse_whenUserIsNotMember() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            UserEntity outsider = persist(user("outsider@example.com", "Outsider"));
            ChatRoomEntity room = persist(newRoom(a.getId(), b.getId()));
            flushAndClear();

            boolean result = chatRoomRepository.hasMemberById(room.getId(), outsider.getId());

            assertThat(result).isFalse();
        }

        @Test
        void returnsFalse_whenRoomDoesNotExist() {
            UserEntity a = persist(user("a@example.com", "A"));
            flushAndClear();

            boolean result = chatRoomRepository.hasMemberById(999L, a.getId());

            assertThat(result).isFalse();
        }
    }


    @Nested
    class CreateBetweenTest {

        @Test
        void ordersUserIds() {
            ChatRoomEntity direct = chatRoomRepository.createBetween(3L, 5L);
            assertThat(direct.getFirstUserId()).isEqualTo(3L);
            assertThat(direct.getSecondUserId()).isEqualTo(5L);

            ChatRoomEntity reversed = chatRoomRepository.createBetween(5L, 3L);
            assertThat(reversed.getFirstUserId()).isEqualTo(3L);
            assertThat(reversed.getSecondUserId()).isEqualTo(5L);
        }

    }


    @Nested
    class FindAllByUserIdTest {

        @Test
        void returnsRoomsWhereUserIsParticipant() {
            UserEntity user = persist(user("user@example.com", "User"));
            UserEntity first = persist(user("first@example.com", "First"));
            UserEntity second = persist(user("second@example.com", "Second"));
            UserEntity outsider = persist(user("outsider@example.com", "Outsider"));

            ChatRoomEntity room1 = persist(newRoom(user.getId(), first.getId()));
            ChatRoomEntity room2 = persist(newRoom(second.getId(), user.getId()));
            persist(newRoom(first.getId(), outsider.getId()));
            flushAndClear();

            Page<ChatRoomEntity> page = chatRoomRepository.findAllByUserId(
                    user.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(page.getContent())
                    .extracting(ChatRoomEntity::getId)
                    .containsExactlyInAnyOrder(room1.getId(), room2.getId());
        }

        @Test
        void ordersByLastMessageAtDesc() {
            UserEntity user = persist(user("user@example.com", "User"));
            UserEntity p1 = persist(user("p1@example.com", "P1"));
            UserEntity p2 = persist(user("p2@example.com", "P2"));
            UserEntity p3 = persist(user("p3@example.com", "P3"));

            ChatRoomEntity oldest = newRoom(user.getId(), p1.getId());
            oldest.setLastMessageAt(Instant.parse("2024-01-01T10:00:00Z"));
            persist(oldest);

            ChatRoomEntity newest = newRoom(user.getId(), p2.getId());
            newest.setLastMessageAt(Instant.parse("2024-03-01T10:00:00Z"));
            persist(newest);

            ChatRoomEntity middle = newRoom(user.getId(), p3.getId());
            middle.setLastMessageAt(Instant.parse("2024-02-01T10:00:00Z"));
            persist(middle);
            flushAndClear();

            Page<ChatRoomEntity> page = chatRoomRepository.findAllByUserId(
                    user.getId(),
                    PageRequest.of(0, 10)
            );

            assertThat(page.getContent())
                    .extracting(ChatRoomEntity::getId)
                    .containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }

        @Test
        void appliesPageable() {
            UserEntity user = persist(user("user@example.com", "User"));
            UserEntity p1 = persist(user("p1@example.com", "P1"));
            UserEntity p2 = persist(user("p2@example.com", "P2"));
            UserEntity p3 = persist(user("p3@example.com", "P3"));

            ChatRoomEntity r1 = newRoom(user.getId(), p1.getId());
            r1.setLastMessageAt(Instant.parse("2024-01-01T10:00:00Z"));
            persist(r1);

            ChatRoomEntity r2 = newRoom(user.getId(), p2.getId());
            r2.setLastMessageAt(Instant.parse("2024-01-02T10:00:00Z"));
            persist(r2);

            ChatRoomEntity r3 = newRoom(user.getId(), p3.getId());
            r3.setLastMessageAt(Instant.parse("2024-01-03T10:00:00Z"));
            persist(r3);
            flushAndClear();

            Page<ChatRoomEntity> firstPage = chatRoomRepository.findAllByUserId(
                    user.getId(),
                    PageRequest.of(0, 2)
            );
            Page<ChatRoomEntity> secondPage = chatRoomRepository.findAllByUserId(
                    user.getId(),
                    PageRequest.of(1, 2)
            );

            assertThat(firstPage.getTotalElements()).isEqualTo(3);
            assertThat(firstPage.getContent())
                    .extracting(ChatRoomEntity::getId)
                    .containsExactly(r3.getId(), r2.getId());
            assertThat(secondPage.getContent())
                    .extracting(ChatRoomEntity::getId)
                    .containsExactly(r1.getId());
        }
    }


    @Nested
    class UpdateLastMessageAtTest {

        @Test
        void updatesTimestamp() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            ChatRoomEntity room = persist(newRoom(a.getId(), b.getId()));
            flushAndClear();

            Instant newTimestamp = Instant.parse("2024-05-01T12:00:00Z");
            chatRoomRepository.updateLastMessageAt(room.getId(), newTimestamp);
            flushAndClear();

            ChatRoomEntity updated = testEntityManager.find(ChatRoomEntity.class, room.getId());
            assertThat(updated.getLastMessageAt()).isEqualTo(newTimestamp);
        }
    }


    @Nested
    class DeleteBetweenTest {

        @Test
        void deletesRoomRegardlessOfOrder() {
            UserEntity a = persist(user("a@example.com", "A"));
            UserEntity b = persist(user("b@example.com", "B"));
            ChatRoomEntity room = persist(newRoom(a.getId(), b.getId()));
            flushAndClear();

            int deleted = chatRoomRepository.deleteBetween(b.getId(), a.getId());
            flushAndClear();

            assertThat(deleted).isEqualTo(1);
            assertThat(testEntityManager.find(ChatRoomEntity.class, room.getId())).isNull();
        }
    }
}