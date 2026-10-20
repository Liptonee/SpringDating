package org.petproject.dating_backend.user;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.BadRequestException;
import org.petproject.dating_backend.common.exception.ConflictException;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.photo.PhotoService;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 123L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PhotoService photoService;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.counter(anyString())).thenReturn(counter);
    }


    private static UserEntity user(UserGender gender, Short age, String city,
                                   String shortAbout, Short prefMin, Short prefMax) {
        UserEntity u = new UserEntity();
        u.setId(USER_ID);
        u.setFirstName("Иван");
        u.setEmail("ivan@example.com");
        u.setPasswordHash("hash");
        u.setReadyForDeck(false);
        u.setGender(gender);
        u.setAge(age);
        u.setCity(city);
        u.setShortAbout(shortAbout);
        u.setPreferredAgeMin(prefMin);
        u.setPreferredAgeMax(prefMax);
        return u;
    }

    private static UserEntity readyUser() {
        UserEntity u = user(UserGender.MALE, (short) 25, "Москва", "Кратко",
                (short) 20, (short) 30);
        u.setFullAbout("Полно");
        return u;
    }

    private static PatchRequestDto emptyPatch() {
        return new PatchRequestDto(null, null, null, null, null, null, null, null, null);
    }

    private static ProfileUserDto toProfileDto(UserEntity e) {
        return new ProfileUserDto(
                e.getId(), e.getFirstName(), e.getEmail(), e.getShortAbout(),
                e.getFullAbout(), e.getAge(), e.getCity(), e.getGender(),
                e.getPreferredAgeMin(), e.getPreferredAgeMax(), e.getReadyForDeck()
        );
    }

    private void stubSuccessfulPatch(UserEntity entity, boolean hasMainPhoto) {
        lenient().when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(entity));
        lenient().when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(entity);
        lenient().when(photoService.hasMainPhoto(USER_ID)).thenReturn(hasMainPhoto);
        lenient().when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userMapper.toProfileDto(any(UserEntity.class)))
                .thenAnswer(inv -> toProfileDto(inv.getArgument(0)));
    }

    @Nested
    class GetCurrentUserTest {

        @Test
        void returnsProfile_whenUserExists() {
            UserEntity entity = readyUser();
            ProfileUserDto dto = toProfileDto(entity);

            when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(entity);
            when(userMapper.toProfileDto(entity)).thenReturn(dto);

            ProfileUserDto result = userService.getCurrentUser(USER_ID);

            assertThat(result).isEqualTo(dto);
            verify(userRepository).findByIdOrElseThrow(USER_ID);
            verify(userMapper).toProfileDto(entity);
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            when(userRepository.findByIdOrElseThrow(USER_ID))
                    .thenThrow(new NotFoundException("Пользователь не найден", 404));

            assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");

            verifyNoInteractions(userMapper);
        }
    }


    @Nested
    class GetUserTest {

        @Test
        void returnsGetDto_whenUserExists() {
            UserEntity entity = readyUser();
            GetUserDto dto = new GetUserDto(USER_ID, "Иван", "Кратко", "Полно",
                    (short) 25, "Москва", UserGender.MALE);

            when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(entity);
            when(userMapper.toGetDto(entity)).thenReturn(dto);

            GetUserDto result = userService.getUser(USER_ID);

            assertThat(result).isEqualTo(dto);
        }

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            when(userRepository.findByIdOrElseThrow(USER_ID))
                    .thenThrow(new NotFoundException("Пользователь не найден", 404));

            assertThatThrownBy(() -> userService.getUser(USER_ID))
                    .isInstanceOf(NotFoundException.class);

            verifyNoInteractions(userMapper);
        }
    }


    @Nested
    class PatchCurrentUserTest {

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.patchCurrentUser(USER_ID, emptyPatch()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Пользователь не найден");

            verify(userRepository, never()).save(any());
        }

        @Test
        void doesNotChangeAnything_whenPatchIsEmpty() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            userService.patchCurrentUser(USER_ID, emptyPatch());

            assertThat(existing.getFirstName()).isEqualTo("Иван");
            assertThat(existing.getEmail()).isEqualTo("ivan@example.com");
            assertThat(existing.getShortAbout()).isEqualTo("Кратко");
            assertThat(existing.getFullAbout()).isEqualTo("Полно");
            assertThat(existing.getAge()).isEqualTo((short) 25);
            assertThat(existing.getCity()).isEqualTo("Москва");
            assertThat(existing.getGender()).isEqualTo(UserGender.MALE);
            assertThat(existing.getPreferredAgeMin()).isEqualTo((short) 20);
            assertThat(existing.getPreferredAgeMax()).isEqualTo((short) 30);
        }

        @Test
        void updatesFirstName_whenProvided() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            PatchRequestDto patch = new PatchRequestDto(
                    "Пётр", null, null, null, null, null, null, null, null);

            userService.patchCurrentUser(USER_ID, patch);

            assertThat(existing.getFirstName()).isEqualTo("Пётр");
            assertThat(existing.getEmail()).isEqualTo("ivan@example.com");
        }

        @Test
        void ignoresFirstName_whenBlank() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            PatchRequestDto patch = new PatchRequestDto(
                    "   ", null, null, null, null, null, null, null, null);

            userService.patchCurrentUser(USER_ID, patch);

            assertThat(existing.getFirstName()).isEqualTo("Иван");
        }

        @Test
        void updatesShortAbout_whenEmptyString() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            PatchRequestDto patch = new PatchRequestDto(
                    null, null, "", null, null, null, null, null, null);

            userService.patchCurrentUser(USER_ID, patch);

            assertThat(existing.getShortAbout()).isEmpty();
        }

        @Test
        void updatesMultipleFields_whenAllProvided() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            PatchRequestDto patch = new PatchRequestDto(
                    "Пётр", null, "Новое кратко", "Новое полно",
                    (short) 30, "Питер", UserGender.FEMALE, (short) 25, (short) 35);

            userService.patchCurrentUser(USER_ID, patch);

            assertThat(existing.getFirstName()).isEqualTo("Пётр");
            assertThat(existing.getShortAbout()).isEqualTo("Новое кратко");
            assertThat(existing.getFullAbout()).isEqualTo("Новое полно");
            assertThat(existing.getAge()).isEqualTo((short) 30);
            assertThat(existing.getCity()).isEqualTo("Питер");
            assertThat(existing.getGender()).isEqualTo(UserGender.FEMALE);
            assertThat(existing.getPreferredAgeMin()).isEqualTo((short) 25);
            assertThat(existing.getPreferredAgeMax()).isEqualTo((short) 35);
        }

        @Test
        void normalizesEmail_whenProvided() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);
            when(userRepository.existsByEmailAndIdNot("new@example.com", USER_ID))
                    .thenReturn(false);

            PatchRequestDto patch = new PatchRequestDto(
                    null, "  New@Example.COM  ", null, null, null, null, null, null, null);

            userService.patchCurrentUser(USER_ID, patch);

            assertThat(existing.getEmail()).isEqualTo("new@example.com");
            verify(userRepository).existsByEmailAndIdNot("new@example.com", USER_ID);
        }

        @Test
        void throwsConflict_whenEmailAlreadyInUse() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);
            when(userRepository.existsByEmailAndIdNot("busy@example.com", USER_ID))
                    .thenReturn(true);

            PatchRequestDto patch = new PatchRequestDto(
                    null, "busy@example.com", null, null, null, null, null, null, null);

            assertThatThrownBy(() -> userService.patchCurrentUser(USER_ID, patch))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Такой email уже существует");

            assertThat(existing.getEmail()).isEqualTo("ivan@example.com");
            verify(userRepository, never()).save(any());
        }

        @Test
        void ignoresEmail_whenBlank() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            PatchRequestDto patch = new PatchRequestDto(
                    null, "   ", null, null, null, null, null, null, null);

            userService.patchCurrentUser(USER_ID, patch);

            assertThat(existing.getEmail()).isEqualTo("ivan@example.com");
            verify(userRepository, never()).existsByEmailAndIdNot(any(), any());
        }

        @Test
        void throwsBadRequest_whenMinGreaterThanMaxInPatch() {
            UserEntity existing = readyUser();
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(existing));

            PatchRequestDto patch = new PatchRequestDto(
                    null, null, null, null, null, null, null, (short) 30, (short) 20);

            assertThatThrownBy(() -> userService.patchCurrentUser(USER_ID, patch))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Минимум диапазона возраста должен быть <= максимума");

            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsBadRequest_whenNewMaxLessThanExistingMin() {
            UserEntity existing = readyUser(); // min=20, max=30
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(existing));

            PatchRequestDto patch = new PatchRequestDto(
                    null, null, null, null, null, null, null, null, (short) 15);

            assertThatThrownBy(() -> userService.patchCurrentUser(USER_ID, patch))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Минимум диапазона возраста должен быть <= максимума");

            verify(userRepository, never()).save(any());
        }

        @Test
        void throwsBadRequest_whenNewMinGreaterThanExistingMax() {
            UserEntity existing = readyUser(); // min=20, max=30
            when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(existing));

            PatchRequestDto patch = new PatchRequestDto(
                    null, null, null, null, null, null, null, (short) 50, null);

            assertThatThrownBy(() -> userService.patchCurrentUser(USER_ID, patch))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Минимум диапазона возраста должен быть <= максимума");

            verify(userRepository, never()).save(any());
        }

        @Test
        void setsReadyForDeckTrue_whenProfileIsCompleteAndHasPhoto() {
            UserEntity existing = readyUser();
            existing.setReadyForDeck(false);
            stubSuccessfulPatch(existing, true);

            ProfileUserDto result = userService.patchCurrentUser(USER_ID, emptyPatch());

            assertThat(existing.getReadyForDeck()).isTrue();
            assertThat(result.readyForDeck()).isTrue();
        }

        @Test
        void keepsReadyForDeckFalse_whenMainPhotoMissing() {
            UserEntity existing = readyUser();
            existing.setReadyForDeck(false);
            stubSuccessfulPatch(existing, false);

            userService.patchCurrentUser(USER_ID, emptyPatch());

            assertThat(existing.getReadyForDeck()).isFalse();
        }

        @Test
        void keepsReadyForDeckFalse_whenCityMissing() {
            UserEntity existing = readyUser();
            existing.setCity(null);
            existing.setReadyForDeck(false);
            stubSuccessfulPatch(existing, true);

            userService.patchCurrentUser(USER_ID, emptyPatch());

            assertThat(existing.getReadyForDeck()).isFalse();
        }

        @Test
        void incrementsCounter_onSuccessfulPatch() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            userService.patchCurrentUser(USER_ID, emptyPatch());

            verify(counter).increment();
        }

        @Test
        void savesEntity_onSuccessfulPatch() {
            UserEntity existing = readyUser();
            stubSuccessfulPatch(existing, false);

            userService.patchCurrentUser(USER_ID, emptyPatch());

            verify(userRepository).save(existing);
        }
    }


    @Nested
    class GetByPreferencesTest {

        @Test
        void throwsNotFound_whenUserDoesNotExist() {
            when(userRepository.findByIdOrElseThrow(USER_ID))
                    .thenThrow(new NotFoundException("Пользователь не найден", 404));

            assertThatThrownBy(() -> userService.getByPreferences(USER_ID, (short) 5))
                    .isInstanceOf(NotFoundException.class);

            verify(userRepository, never())
                    .findByPreferences(any(), any(), any(), any(), any(), any());
        }

        @Test
        void throwsForbidden_whenProfileNotReady() {
            UserEntity notReady = new UserEntity();
            notReady.setId(USER_ID); // всё остальное null

            when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(notReady);
            when(photoService.hasMainPhoto(USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> userService.getByPreferences(USER_ID, (short) 5))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Профиль пользователя не готов к получению колоды");

            verify(userRepository, never())
                    .findByPreferences(any(), any(), any(), any(), any(), any());
        }

        @Test
        void queriesOppositeGender_withUserPreferences() {
            UserEntity ready = readyUser(); // MALE, 25, Москва, 20–30

            when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(ready);
            when(photoService.hasMainPhoto(USER_ID)).thenReturn(true);
            when(userRepository.findByPreferences(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            List<GetUserDto> result = userService.getByPreferences(USER_ID, (short) 5);

            assertThat(result).isEmpty();
            verify(userRepository).findByPreferences(
                    eq(USER_ID),
                    eq(UserGender.FEMALE),
                    eq((short) 20),
                    eq((short) 30),
                    eq("Москва"),
                    argThat((Pageable p) -> p.getPageNumber() == 0 && p.getPageSize() == 5)
            );
        }

        @Test
        void mapsEntitiesToDtos() {
            UserEntity ready = readyUser();
            UserEntity other = new UserEntity();
            other.setId(999L);
            other.setFirstName("Мария");

            GetUserDto dto = new GetUserDto(999L, "Мария", null, null,
                    (short) 22, "Москва", UserGender.FEMALE);

            when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(ready);
            when(photoService.hasMainPhoto(USER_ID)).thenReturn(true);
            when(userRepository.findByPreferences(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(other));
            when(userMapper.toGetDto(other)).thenReturn(dto);

            List<GetUserDto> result = userService.getByPreferences(USER_ID, (short) 5);

            assertThat(result).containsExactly(dto);
        }
    }


    @Nested
    class GetNotReadyFieldsTest {

        @ParameterizedTest
        @MethodSource("provideCases_GetNotReadyFields")
        void returnsCorrectSet(UserEntity user, boolean hasMainPhoto, Set<String> expected) {
            when(userRepository.findByIdOrElseThrow(USER_ID)).thenReturn(user);
            when(photoService.hasMainPhoto(USER_ID)).thenReturn(hasMainPhoto);

            Set<String> actual = userService.getNotReadyFields(USER_ID);

            assertThat(actual).isEqualTo(expected);
        }

        static Stream<Arguments> provideCases_GetNotReadyFields() {
            return Stream.of(
                    // всё отсутствует
                    Arguments.of(user(null, null, null, null, null, null), false,
                            Set.of("gender", "age", "city", "short about",
                                    "main photo", "preferred age min", "preferred age max")),
                    // всё заполнено, фото есть
                    Arguments.of(user(UserGender.MALE, (short) 25, "Москва", "Кратко",
                            (short) 20, (short) 30), true, Set.of()),
                    // не хватает age
                    Arguments.of(user(UserGender.MALE, null, "Москва", "Кратко",
                            (short) 20, (short) 30), true, Set.of("age")),
                    // не хватает gender
                    Arguments.of(user(null, (short) 25, "Москва", "Кратко",
                            (short) 20, (short) 30), true, Set.of("gender")),
                    // не хватает city
                    Arguments.of(user(UserGender.MALE, (short) 25, null, "Кратко",
                            (short) 20, (short) 30), true, Set.of("city")),
                    // не хватает shortAbout
                    Arguments.of(user(UserGender.MALE, (short) 25, "Москва", null,
                            (short) 20, (short) 30), true, Set.of("short about")),
                    // не хватает preferredAgeMin
                    Arguments.of(user(UserGender.MALE, (short) 25, "Москва", "Кратко",
                            null, (short) 30), true, Set.of("preferred age min")),
                    // не хватает preferredAgeMax
                    Arguments.of(user(UserGender.MALE, (short) 25, "Москва", "Кратко",
                            (short) 20, null), true, Set.of("preferred age max")),
                    // нет главного фото
                    Arguments.of(user(UserGender.MALE, (short) 25, "Москва", "Кратко",
                            (short) 20, (short) 30), false, Set.of("main photo")),
                    // нет фото и обоих preferredAge
                    Arguments.of(user(UserGender.FEMALE, (short) 25, "Москва", "Кратко",
                                    null, null), false,
                            Set.of("main photo", "preferred age min", "preferred age max"))
            );
        }
    }


    @Nested
    class GetFirstNamesByIdsTest {

        @Test
        void returnsEmptyMap_whenIdsIsNull() {
            Map<Long, String> result = userService.getFirstNamesByIds(null);

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void returnsEmptyMap_whenIdsIsEmpty() {
            Map<Long, String> result = userService.getFirstNamesByIds(List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        void returnsIdToFirstNameMap() {
            when(userRepository.findFirstNamesByIds(List.of(1L, 2L)))
                    .thenReturn(List.of(
                            projection(1L, "Иван"),
                            projection(2L, "Пётр")
                    ));

            Map<Long, String> result = userService.getFirstNamesByIds(List.of(1L, 2L));

            assertThat(result)
                    .hasSize(2)
                    .containsEntry(1L, "Иван")
                    .containsEntry(2L, "Пётр");
        }

        private static UserRepository.UserNameProjection projection(Long id, String name) {
            return new UserRepository.UserNameProjection() {
                @Override
                public Long getId() {
                    return id;
                }

                @Override
                public String getFirstName() {
                    return name;
                }
            };
        }
    }
}