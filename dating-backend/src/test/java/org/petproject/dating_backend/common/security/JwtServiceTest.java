package org.petproject.dating_backend.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.user.UserEntity;
import org.petproject.dating_backend.user.UserRole;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final Long USER_ID = 123L;
    private static final String EMAIL = "ivan@example.com";

    // Base64 от 32-байтового секрета: "12345678901234567890123456789012"
    private static final String SECRET = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
    private static final Long EXPIRATION_MS = 60_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    private static UserEntity user() {
        UserEntity u = new UserEntity();
        u.setId(USER_ID);
        u.setEmail(EMAIL);
        u.setRole(UserRole.USER);
        return u;
    }

    @Nested
    class GenerateTokenTest {

        @Test
        void generatesTokenWithSubjectUserIdAndRoles() {
            UserEntity user = user();

            String token = jwtService.generateToken(user);

            assertThat(token).isNotBlank();

            Claims claims = jwtService.parseClaims(token);

            assertThat(claims.getSubject()).isEqualTo(EMAIL);
            assertThat(claims.get(JwtClaims.USER_ID, Long.class)).isEqualTo(USER_ID);
            assertThat(claims.get(JwtClaims.ROLES, List.class))
                    .containsExactly(UserRole.USER.name());
        }

        @Test
        void setsExpirationInFuture() {
            String token = jwtService.generateToken(user());

            Claims claims = jwtService.parseClaims(token);

            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        }
    }

    @Nested
    class ExtractUserIdTest {

        @Test
        void returnsUserIdFromToken() {
            String token = jwtService.generateToken(user());

            Long result = jwtService.extractUserId(token);

            assertThat(result).isEqualTo(USER_ID);
        }
    }

    @Nested
    class ExtractRolesTest {

        @Test
        void returnsRolesFromToken() {
            String token = jwtService.generateToken(user());

            List<String> result = jwtService.extractRoles(token);

            assertThat(result).containsExactly(UserRole.USER.name());
        }
    }

    @Nested
    class ParseClaimsTest {

        @Test
        void returnsClaimsForValidToken() {
            String token = jwtService.generateToken(user());

            Claims claims = jwtService.parseClaims(token);

            assertThat(claims.getSubject()).isEqualTo(EMAIL);
            assertThat(claims.get(JwtClaims.USER_ID, Long.class)).isEqualTo(USER_ID);
            assertThat(claims.get(JwtClaims.ROLES, List.class))
                    .containsExactly(UserRole.USER.name());
        }
    }

    @Nested
    class IsExpiredTest {

        @Test
        void returnsFalseForValidToken() {
            String token = jwtService.generateToken(user());

            boolean result = jwtService.isExpired(token);

            assertThat(result).isFalse();
        }

        @Test
        void throwsExpiredJwtExceptionWhenTokenExpired() {
            JwtService expiredJwtService = new JwtService(SECRET, -1_000L);
            String token = expiredJwtService.generateToken(user());

            assertThatThrownBy(() -> jwtService.isExpired(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }
    }
}