package org.petproject.dating_backend.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserGender;
import org.petproject.dating_backend.user.UserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String BASE64_SECRET
            = "N2YzYzRkNWU2ZjdhOGI5YzBkMWUyZjNhNGI1YzZkN2U4ZjlhMGIxYzJkM2U0ZjVhNmI3YzhkOWUwZjFhMmIzYzRkNWU2Zjc4";
    private static final long TTL_MS = 3_600_000;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(BASE64_SECRET, TTL_MS);
    }

    private static UserEntity readyUser() {
        UserEntity u = new UserEntity();
        u.setId(123L);
        u.setFirstName("Иван");
        u.setEmail("ivan@example.com");
        u.setPasswordHash("hash");
        u.setReadyForDeck(true);
        u.setGender(UserGender.MALE);
        u.setAge((short) 25);
        u.setCity("Москва");
        u.setShortAbout("Кратко");
        u.setPreferredAgeMin((short) 20);
        u.setPreferredAgeMax((short) 30);
        u.setFullAbout("Полно");
        u.setRole(UserRole.USER);
        return u;
    }

    @Nested
    class GenerateTokenTest{

        @Test
        void returnsCorrectToken_correctInput() {

            String token = jwtService.generateToken(readyUser());
            Claims claims = jwtService.parseClaims(token);

            assertThat(claims.getSubject()).isEqualTo("ivan@example.com");
            assertThat(claims.get(JwtClaims.ROLES, List.class))
                    .containsExactly("USER");
            assertThat(claims.get(JwtClaims.USER_ID, Long.class)).isEqualTo(123L);
        }



    }

}