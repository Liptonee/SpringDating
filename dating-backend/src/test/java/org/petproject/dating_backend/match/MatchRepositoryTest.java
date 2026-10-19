package org.petproject.dating_backend.match;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MatchRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MatchRepository matchRepository;

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

    private MatchEntity persistMatch(Long a, Long b) {
        return persistMatch(a, b, Instant.now());
    }

    private MatchEntity persistMatch(Long a, Long b, Instant createdAt) {
        MatchEntity m = new MatchEntity();
        m.setFirstUserId(Math.min(a, b));
        m.setSecondUserId(Math.max(a, b));
        m.setCreatedAt(createdAt);
        return testEntityManager.persistAndFlush(m);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.getEntityManager().clear();
    }


    @Nested
    class ExistsByPairTest {

        @Test
        void returnsTrue_whenPairExists() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            long smaller = Math.min(a.getId(), b.getId());
            long larger = Math.max(a.getId(), b.getId());

            persistMatch(a.getId(), b.getId());
            flushAndClear();

            boolean result = matchRepository.existsByPair(smaller, larger);

            assertThat(result).isTrue();
        }

        @Test
        void returnsFalse_whenPairDoesNotExist() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            flushAndClear();

            boolean result = matchRepository.existsByPair(a.getId(), b.getId());

            assertThat(result).isFalse();
        }

        @Test
        void returnsFalse_whenArgumentsReversed() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            long smaller = Math.min(a.getId(), b.getId());
            long larger = Math.max(a.getId(), b.getId());

            persistMatch(smaller, larger);
            flushAndClear();

            boolean result = matchRepository.existsByPair(larger, smaller);

            assertThat(result).isFalse();
        }
    }


    @Nested
    class ExistsBetweenTest {

        @Test
        void returnsTrue_whenPairExists_sameOrder() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            long smaller = Math.min(a.getId(), b.getId());
            long larger = Math.max(a.getId(), b.getId());

            persistMatch(a.getId(), b.getId());
            flushAndClear();

            boolean result = matchRepository.existsBetween(smaller, larger);

            assertThat(result).isTrue();
        }

        @Test
        void returnsTrue_whenPairExists_reversedOrder() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            long smaller = Math.min(a.getId(), b.getId());
            long larger = Math.max(a.getId(), b.getId());

            persistMatch(a.getId(), b.getId());
            flushAndClear();

            boolean result = matchRepository.existsBetween(larger, smaller);

            assertThat(result).isTrue();
        }

        @Test
        void returnsFalse_whenNoMatch() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            flushAndClear();

            boolean result = matchRepository.existsBetween(a.getId(), b.getId());

            assertThat(result).isFalse();
        }
    }


    @Nested
    class CreateBetweenTest {

        @Test
        void setsIdsInAscendingOrder() {
            MatchEntity m = matchRepository.createBetween(1L, 5L);

            assertThat(m.getFirstUserId()).isEqualTo(1L);
            assertThat(m.getSecondUserId()).isEqualTo(5L);
        }

        @Test
        void normalizesReversedInput() {
            MatchEntity m = matchRepository.createBetween(5L, 1L);

            assertThat(m.getFirstUserId()).isEqualTo(1L);
            assertThat(m.getSecondUserId()).isEqualTo(5L);
        }
    }


    @Nested
    class DeleteBetweenTest {

        @Test
        void deletesPair_sameOrder() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            long smaller = Math.min(a.getId(), b.getId());
            long larger = Math.max(a.getId(), b.getId());

            persistMatch(a.getId(), b.getId());
            flushAndClear();

            int deleted = matchRepository.deleteBetween(smaller, larger);
            flushAndClear();

            assertThat(deleted).isEqualTo(1);
            assertThat(matchRepository.existsByPair(smaller, larger)).isFalse();
        }

        @Test
        void deletesPair_reversedOrder() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            long smaller = Math.min(a.getId(), b.getId());
            long larger = Math.max(a.getId(), b.getId());

            persistMatch(a.getId(), b.getId());
            flushAndClear();

            int deleted = matchRepository.deleteBetween(larger, smaller);
            flushAndClear();

            assertThat(deleted).isEqualTo(1);
            assertThat(matchRepository.existsByPair(smaller, larger)).isFalse();
        }

        @Test
        void returnsZero_whenNoMatch() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            flushAndClear();

            int deleted = matchRepository.deleteBetween(a.getId(), b.getId());

            assertThat(deleted).isZero();
        }
    }


    @Nested
    class FindAllBySingleUserIdTest {

        @Test
        void returnsMatch_whenUserIsFirst() {
            UserEntity user = persistUser("user@example.com");
            UserEntity other = persistUser("other@example.com");
            MatchEntity match = persistMatch(user.getId(), other.getId());
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(MatchEntity::getId)
                    .containsExactly(match.getId());
        }

        @Test
        void returnsMatch_whenUserIsSecond() {
            UserEntity user = persistUser("user@example.com");
            UserEntity other = persistUser("other@example.com");
            MatchEntity match = persistMatch(other.getId(), user.getId());
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(MatchEntity::getId)
                    .containsExactly(match.getId());
        }

        @Test
        void returnsAllMatches_whenUserInMultipleMatches() {
            UserEntity user = persistUser("user@example.com");
            UserEntity other1 = persistUser("other1@example.com");
            UserEntity other2 = persistUser("other2@example.com");
            MatchEntity m1 = persistMatch(user.getId(), other1.getId());
            MatchEntity m2 = persistMatch(user.getId(), other2.getId());
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(MatchEntity::getId)
                    .containsExactlyInAnyOrder(m1.getId(), m2.getId());
        }

        @Test
        void excludesMatchesOfOtherUsers() {
            UserEntity user = persistUser("user@example.com");
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            persistMatch(a.getId(), b.getId());
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        void ordersByCreatedAtDesc() {
            UserEntity user = persistUser("user@example.com");
            UserEntity o1 = persistUser("o1@example.com");
            UserEntity o2 = persistUser("o2@example.com");
            UserEntity o3 = persistUser("o3@example.com");

            MatchEntity older = persistMatch(user.getId(), o1.getId(),
                    Instant.parse("2024-01-01T00:00:00Z"));
            MatchEntity newest = persistMatch(user.getId(), o2.getId(),
                    Instant.parse("2024-03-01T00:00:00Z"));
            MatchEntity middle = persistMatch(user.getId(), o3.getId(),
                    Instant.parse("2024-02-01T00:00:00Z"));
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent())
                    .extracting(MatchEntity::getId)
                    .containsExactly(newest.getId(), middle.getId(), older.getId());
        }

        @Test
        void appliesPageable() {
            UserEntity user = persistUser("user@example.com");
            UserEntity o1 = persistUser("o1@example.com");
            UserEntity o2 = persistUser("o2@example.com");
            UserEntity o3 = persistUser("o3@example.com");
            persistMatch(user.getId(), o1.getId(), Instant.parse("2024-01-01T00:00:00Z"));
            persistMatch(user.getId(), o2.getId(), Instant.parse("2024-02-01T00:00:00Z"));
            persistMatch(user.getId(), o3.getId(), Instant.parse("2024-03-01T00:00:00Z"));
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 2)
            );

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(3L);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }

        @Test
        void returnsEmpty_whenNoMatches() {
            UserEntity user = persistUser("user@example.com");
            flushAndClear();

            Page<MatchEntity> result = matchRepository.findAllBySingleUserId(
                    user.getId(), PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }


    @Nested
    class ConstraintsTest {

        @Test
        void uniqueConstraint_preventsDuplicatePair() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");
            persistMatch(a.getId(), b.getId());
            flushAndClear();

            MatchEntity duplicate = new MatchEntity();
            duplicate.setFirstUserId(Math.min(a.getId(), b.getId()));
            duplicate.setSecondUserId(Math.max(a.getId(), b.getId()));

            assertThatThrownBy(() -> matchRepository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void checkConstraint_preventsFirstGreaterThanSecond() {
            UserEntity a = persistUser("a@example.com");
            UserEntity b = persistUser("b@example.com");

            MatchEntity reversed = new MatchEntity();
            reversed.setFirstUserId(Math.max(a.getId(), b.getId()));
            reversed.setSecondUserId(Math.min(a.getId(), b.getId()));

            assertThatThrownBy(() -> matchRepository.saveAndFlush(reversed))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}