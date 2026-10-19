package org.petproject.dating_backend.swipe;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SwipeRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SwipeRepository swipeRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    private UserEntity persistUser(String email) {
        UserEntity u = new UserEntity();
        u.setFirstName("User");
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
        return testEntityManager.persistAndFlush(u);
    }

    private SwipeEntity persist(Long fromId, Long toId, SwipeAction action) {
        return persist(fromId, toId, action, Instant.now());
    }

    private SwipeEntity persist(Long fromId, Long toId, SwipeAction action, Instant createdAt) {
        SwipeEntity s = new SwipeEntity();
        s.setFromId(fromId);
        s.setToId(toId);
        s.setAction(action);
        s.setCreatedAt(createdAt);
        return testEntityManager.persistAndFlush(s);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.getEntityManager().clear();
    }


    @Nested
    class ExistsByFromIdAndToIdTest {

        @Test
        void returnsTrue_whenSwipeExists() {
            UserEntity from = persistUser("from@example.com");
            UserEntity to = persistUser("to@example.com");
            persist(from.getId(), to.getId(), SwipeAction.LIKE);
            flushAndClear();

            boolean result = swipeRepository.existsByFromIdAndToId(from.getId(), to.getId());

            assertThat(result).isTrue();
        }

        @Test
        void returnsFalse_whenSwipeDoesNotExist() {
            UserEntity from = persistUser("from@example.com");
            UserEntity to = persistUser("to@example.com");
            flushAndClear();

            boolean result = swipeRepository.existsByFromIdAndToId(from.getId(), to.getId());

            assertThat(result).isFalse();
        }

        @Test
        void returnsFalse_whenFromAndToAreReversed() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            persist(a.getId(), b.getId(), SwipeAction.LIKE);
            flushAndClear();

            boolean result = swipeRepository.existsByFromIdAndToId(b.getId(), a.getId());

            assertThat(result).isFalse();
        }
    }


    @Nested
    class FindAllForHistoryTest {

        @Test
        void returnsAllUserSwipes_whenActionIsNull() {
            UserEntity from = persistUser("from@example.com");
            UserEntity to1 = persistUser("to1@example.com");
            UserEntity to2 = persistUser("to2@example.com");
            persist(from.getId(), to1.getId(), SwipeAction.LIKE);
            persist(from.getId(), to2.getId(), SwipeAction.DISLIKE);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForHistory(
                    from.getId(), null, PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        void filtersByAction_whenActionProvided() {
            UserEntity from = persistUser("from@example.com");
            UserEntity to1 = persistUser("to1@example.com");
            UserEntity to2 = persistUser("to2@example.com");
            SwipeEntity like = persist(from.getId(), to1.getId(), SwipeAction.LIKE);
            persist(from.getId(), to2.getId(), SwipeAction.DISLIKE);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForHistory(
                    from.getId(), SwipeAction.LIKE, PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(SwipeEntity::getId)
                    .containsExactly(like.getId());
        }

        @Test
        void excludesSwipesOfOtherUsers() {
            UserEntity user = persistUser("user@example.com");
            UserEntity other = persistUser("other@example.com");
            UserEntity target = persistUser("target@example.com");

            SwipeEntity own = persist(user.getId(), target.getId(), SwipeAction.LIKE);
            persist(other.getId(), target.getId(), SwipeAction.LIKE);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForHistory(
                    user.getId(), null, PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(SwipeEntity::getId)
                    .containsExactly(own.getId());
        }

        @Test
        void ordersByCreatedAtDesc() {
            UserEntity from = persistUser("from@example.com");
            UserEntity to1 = persistUser("to1@example.com");
            UserEntity to2 = persistUser("to2@example.com");
            UserEntity to3 = persistUser("to3@example.com");

            Instant base = Instant.parse("2024-01-01T10:00:00Z");
            SwipeEntity oldest = persist(from.getId(), to1.getId(), SwipeAction.LIKE, base.minusSeconds(120));
            SwipeEntity middle = persist(from.getId(), to2.getId(), SwipeAction.DISLIKE, base.minusSeconds(60));
            SwipeEntity newest = persist(from.getId(), to3.getId(), SwipeAction.LIKE, base);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForHistory(
                    from.getId(), null, PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(SwipeEntity::getId)
                    .containsExactly(newest.getId(), middle.getId(), oldest.getId());
        }

        @Test
        void appliesPageable() {
            UserEntity from = persistUser("from@example.com");
            UserEntity to1 = persistUser("to1@example.com");
            UserEntity to2 = persistUser("to2@example.com");
            UserEntity to3 = persistUser("to3@example.com");
            persist(from.getId(), to1.getId(), SwipeAction.LIKE);
            persist(from.getId(), to2.getId(), SwipeAction.LIKE);
            persist(from.getId(), to3.getId(), SwipeAction.LIKE);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForHistory(
                    from.getId(), null, PageRequest.of(0, 2)
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }


    @Nested
    class FindAllForLikedTest {

        @Test
        void returnsOnlyLikesToUser() {
            UserEntity target = persistUser("target@example.com");
            UserEntity liker = persistUser("liker@example.com");
            UserEntity disliker = persistUser("disliker@example.com");
            UserEntity otherTarget = persistUser("other-target@example.com");

            SwipeEntity liked = persist(liker.getId(), target.getId(), SwipeAction.LIKE);
            persist(disliker.getId(), target.getId(), SwipeAction.DISLIKE);
            persist(liker.getId(), otherTarget.getId(), SwipeAction.LIKE);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForLiked(
                    target.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(SwipeEntity::getId)
                    .containsExactly(liked.getId());
        }

        @Test
        void ordersByCreatedAtDesc() {
            UserEntity target = persistUser("target@example.com");
            UserEntity liker1 = persistUser("liker1@example.com");
            UserEntity liker2 = persistUser("liker2@example.com");

            Instant base = Instant.parse("2024-01-01T10:00:00Z");
            SwipeEntity oldest = persist(liker1.getId(), target.getId(), SwipeAction.LIKE, base.minusSeconds(120));
            SwipeEntity newest = persist(liker2.getId(), target.getId(), SwipeAction.LIKE, base);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForLiked(
                    target.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(SwipeEntity::getId)
                    .containsExactly(newest.getId(), oldest.getId());
        }

        @Test
        void appliesPageable() {
            UserEntity target = persistUser("target@example.com");
            UserEntity liker1 = persistUser("liker1@example.com");
            UserEntity liker2 = persistUser("liker2@example.com");
            UserEntity liker3 = persistUser("liker3@example.com");
            persist(liker1.getId(), target.getId(), SwipeAction.LIKE);
            persist(liker2.getId(), target.getId(), SwipeAction.LIKE);
            persist(liker3.getId(), target.getId(), SwipeAction.LIKE);
            flushAndClear();

            Page<SwipeEntity> result = swipeRepository.findAllForLiked(
                    target.getId(), PageRequest.of(0, 2)
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }
    }
}