package org.petproject.dating_backend.user;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager testEntityManager;


    private UserEntity persist(UserEntity user) {
        return testEntityManager.persistAndFlush(user);
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.getEntityManager().clear();
    }

    private void persistSwipe(UserEntity from, UserEntity to) {
        SwipeEntity swipe = new SwipeEntity();
        swipe.setFromId(from.getId());
        swipe.setToId(to.getId());
        swipe.setAction(SwipeAction.LIKE);
        testEntityManager.persistAndFlush(swipe);
    }

    private static UserEntity user(String email,
                                   String firstName,
                                   UserGender gender,
                                   Short age,
                                   String city,
                                   boolean readyForDeck) {
        UserEntity u = new UserEntity();
        u.setFirstName(firstName);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setReadyForDeck(readyForDeck);
        u.setGender(gender);
        u.setAge(age);
        u.setCity(city);
        u.setShortAbout("Кратко");
        u.setFullAbout("Полно");
        u.setPreferredAgeMin((short) 20);
        u.setPreferredAgeMax((short) 30);
        u.setRole(UserRole.USER);
        return u;
    }


    @Nested
    class FindByEmailTest {

        @Test
        void returnsUser_whenExists() {
            UserEntity saved = persist(user(
                    "ivan@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            flushAndClear();

            Optional<UserEntity> result = userRepository.findByEmail("ivan@example.com");

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        void returnsEmpty_whenUserDoesNotExist() {
            Optional<UserEntity> result = userRepository.findByEmail("missing@example.com");

            assertThat(result).isEmpty();
        }
    }


    @Nested
    class ExistsByEmailTest {

        @Test
        void returnsTrue_whenEmailExists() {
            persist(user(
                    "ivan@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            flushAndClear();

            boolean result = userRepository.existsByEmail("ivan@example.com");

            assertThat(result).isTrue();
        }

        @Test
        void returnsFalse_whenEmailDoesNotExist() {
            boolean result = userRepository.existsByEmail("missing@example.com");

            assertThat(result).isFalse();
        }
    }


    @Nested
    class FindByEmailOrThrowTest {

        @Test
        void returnsUser_whenExists() {
            UserEntity saved = persist(user(
                    "ivan@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            flushAndClear();

            UserEntity result = userRepository.findByEmailOrThrow("ivan@example.com");

            assertThat(result.getId()).isEqualTo(saved.getId());
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            assertThatThrownBy(() -> userRepository.findByEmailOrThrow("missing@example.com"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");
        }
    }


    @Nested
    class FindByIdOrElseThrowTest {

        @Test
        void returnsUser_whenExists() {
            UserEntity saved = persist(user(
                    "ivan@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            flushAndClear();

            UserEntity result = userRepository.findByIdOrElseThrow(saved.getId());

            assertThat(result.getId()).isEqualTo(saved.getId());
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            assertThatThrownBy(() -> userRepository.findByIdOrElseThrow(999L))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");
        }
    }


    @Nested
    class ExistsByEmailAndIdNotTest {

        @Test
        void returnsTrue_whenEmailBelongsToAnotherUser() {
            UserEntity first = persist(user(
                    "first@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            UserEntity second = persist(user(
                    "second@example.com", "Пётр", UserGender.MALE,
                    (short) 26, "Москва", true
            ));
            flushAndClear();

            boolean result = userRepository.existsByEmailAndIdNot(
                    "first@example.com",
                    second.getId()
            );

            assertThat(result).isTrue();
            assertThat(first.getId()).isNotEqualTo(second.getId());
        }

        @Test
        void returnsFalse_whenEmailBelongsToSameUser() {
            UserEntity first = persist(user(
                    "first@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            flushAndClear();

            boolean result = userRepository.existsByEmailAndIdNot(
                    "first@example.com",
                    first.getId()
            );

            assertThat(result).isFalse();
        }
    }


    @Nested
    class FindByIdForUpdateTest {

        @Test
        void returnsUser_whenExists() {
            UserEntity saved = persist(user(
                    "ivan@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            flushAndClear();

            Optional<UserEntity> result = userRepository.findByIdForUpdate(saved.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(saved.getId());
        }

        @Test
        void returnsEmpty_whenUserDoesNotExist() {
            Optional<UserEntity> result = userRepository.findByIdForUpdate(999L);

            assertThat(result).isEmpty();
        }
    }


    @Nested
    class FindByPreferencesTest {

        @Test
        void returnsOnlyMatchingAndNotSwipedUsers() {
            UserEntity current = persist(user(
                    "current@example.com", "Current", UserGender.MALE,
                    (short) 25, "Москва", true
            ));

            UserEntity match = persist(user(
                    "match@example.com", "Match", UserGender.FEMALE,
                    (short) 25, "Москва", true
            ));

            persist(user(
                    "wrong-gender@example.com", "WrongGender", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            persist(user(
                    "wrong-city@example.com", "WrongCity", UserGender.FEMALE,
                    (short) 25, "Питер", true
            ));
            persist(user(
                    "too-young@example.com", "TooYoung", UserGender.FEMALE,
                    (short) 19, "Москва", true
            ));
            persist(user(
                    "too-old@example.com", "TooOld", UserGender.FEMALE,
                    (short) 31, "Москва", true
            ));
            persist(user(
                    "not-ready@example.com", "NotReady", UserGender.FEMALE,
                    (short) 25, "Москва", false
            ));

            UserEntity swiped = persist(user(
                    "swiped@example.com", "Swiped", UserGender.FEMALE,
                    (short) 25, "Москва", true
            ));
            persistSwipe(current, swiped);

            flushAndClear();

            List<UserEntity> result = userRepository.findByPreferences(
                    current.getId(),
                    UserGender.FEMALE,
                    (short) 20,
                    (short) 30,
                    "Москва",
                    PageRequest.of(0, 10)
            );

            assertThat(result)
                    .extracting(UserEntity::getId)
                    .containsExactly(match.getId());
        }

        @Test
        void appliesPageable() {
            UserEntity current = persist(user(
                    "current@example.com", "Current", UserGender.MALE,
                    (short) 25, "Москва", true
            ));

            persist(user(
                    "match-1@example.com", "Match1", UserGender.FEMALE,
                    (short) 24, "Москва", true
            ));
            persist(user(
                    "match-2@example.com", "Match2", UserGender.FEMALE,
                    (short) 26, "Москва", true
            ));

            flushAndClear();

            List<UserEntity> result = userRepository.findByPreferences(
                    current.getId(),
                    UserGender.FEMALE,
                    (short) 20,
                    (short) 30,
                    "Москва",
                    PageRequest.of(0, 1)
            );

            assertThat(result).hasSize(1);
        }
    }


    @Nested
    class FindFirstNamesByIdsTest {

        @Test
        void returnsProjectionsForExistingIds() {
            UserEntity first = persist(user(
                    "first@example.com", "Иван", UserGender.MALE,
                    (short) 25, "Москва", true
            ));
            UserEntity second = persist(user(
                    "second@example.com", "Пётр", UserGender.MALE,
                    (short) 26, "Москва", true
            ));
            flushAndClear();

            List<UserRepository.UserNameProjection> result =
                    userRepository.findFirstNamesByIds(
                            List.of(first.getId(), second.getId(), 999L)
                    );

            assertThat(result)
                    .extracting(
                            UserRepository.UserNameProjection::getId,
                            UserRepository.UserNameProjection::getFirstName
                    )
                    .containsExactlyInAnyOrder(
                            tuple(first.getId(), "Иван"),
                            tuple(second.getId(), "Пётр")
                    );
        }
    }
}
