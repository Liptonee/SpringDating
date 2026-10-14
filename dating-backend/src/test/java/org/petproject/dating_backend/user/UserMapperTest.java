package org.petproject.dating_backend.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private static final Long USER_ID = 123L;

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
    }

    private static UserEntity fullUser() {
        UserEntity u = new UserEntity();
        u.setId(USER_ID);
        u.setFirstName("Иван");
        u.setEmail("ivan@example.com");
        u.setShortAbout("Кратко");
        u.setFullAbout("Полно");
        u.setAge((short) 25);
        u.setCity("Москва");
        u.setGender(UserGender.MALE);
        u.setPreferredAgeMin((short) 20);
        u.setPreferredAgeMax((short) 30);
        u.setReadyForDeck(true);
        return u;
    }

    @Nested
    class ToProfileDtoTest {

        @Test
        void returnsNull_whenEntityIsNull() {
            assertThat(userMapper.toProfileDto(null)).isNull();
        }

        @Test
        void mapsAllFields_whenEntityIsFull() {
            UserEntity entity = fullUser();

            ProfileUserDto dto = userMapper.toProfileDto(entity);

            assertThat(dto).isEqualTo(new ProfileUserDto(
                    USER_ID,
                    "Иван",
                    "ivan@example.com",
                    "Кратко",
                    "Полно",
                    (short) 25,
                    "Москва",
                    UserGender.MALE,
                    (short) 20,
                    (short) 30,
                    true
            ));
        }

        @Test
        void mapsOnlyId_whenOtherFieldsAreNull() {
            UserEntity entity = new UserEntity();
            entity.setId(USER_ID);

            ProfileUserDto dto = userMapper.toProfileDto(entity);

            assertThat(dto.id()).isEqualTo(USER_ID);
            assertThat(dto.firstName()).isNull();
            assertThat(dto.email()).isNull();
            assertThat(dto.shortAbout()).isNull();
            assertThat(dto.fullAbout()).isNull();
            assertThat(dto.age()).isNull();
            assertThat(dto.city()).isNull();
            assertThat(dto.gender()).isNull();
            assertThat(dto.preferredAgeMin()).isNull();
            assertThat(dto.preferredAgeMax()).isNull();
            assertThat(dto.readyForDeck()).isNull();
        }

        @Test
        void mapsReadyForDeckFalse() {
            UserEntity entity = fullUser();
            entity.setReadyForDeck(false);

            ProfileUserDto dto = userMapper.toProfileDto(entity);

            assertThat(dto.readyForDeck()).isFalse();
        }
    }


    @Nested
    class ToGetDtoTest {

        @Test
        void returnsNull_whenEntityIsNull() {
            assertThat(userMapper.toGetDto(null)).isNull();
        }

        @Test
        void mapsAllFields_whenEntityIsFull() {
            UserEntity entity = fullUser();

            GetUserDto dto = userMapper.toGetDto(entity);

            assertThat(dto).isEqualTo(new GetUserDto(
                    USER_ID,
                    "Иван",
                    "Кратко",
                    "Полно",
                    (short) 25,
                    "Москва",
                    UserGender.MALE
            ));
        }

        @Test
        void mapsOnlyId_whenOtherFieldsAreNull() {
            UserEntity entity = new UserEntity();
            entity.setId(USER_ID);

            GetUserDto dto = userMapper.toGetDto(entity);

            assertThat(dto.id()).isEqualTo(USER_ID);
            assertThat(dto.firstName()).isNull();
            assertThat(dto.shortAbout()).isNull();
            assertThat(dto.fullAbout()).isNull();
            assertThat(dto.age()).isNull();
            assertThat(dto.city()).isNull();
            assertThat(dto.gender()).isNull();
        }

        @Test
        void ignoresFieldsThatAreNotInGetUserDto() {
            UserEntity entity = fullUser();

            GetUserDto dto = userMapper.toGetDto(entity);

            assertThat(dto.id()).isEqualTo(USER_ID);
            assertThat(dto.firstName()).isEqualTo("Иван");
            assertThat(dto.shortAbout()).isEqualTo("Кратко");
            assertThat(dto.fullAbout()).isEqualTo("Полно");
            assertThat(dto.age()).isEqualTo((short) 25);
            assertThat(dto.city()).isEqualTo("Москва");
            assertThat(dto.gender()).isEqualTo(UserGender.MALE);
        }
    }
}