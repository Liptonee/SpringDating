package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.auth.LoginRequestDto;
import org.petproject.dating_backend.auth.RegisterRequestDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthNegativeIT extends BaseIntegrationTest {

    @Test
    void register_duplicateEmail_returns409() {
        register(EMAIL, FIRST_NAME, PASSWORD);

        RegisterRequestDto duplicate = new RegisterRequestDto(
                EMAIL.toUpperCase(),
                FIRST_NAME,
                PASSWORD
        );

        restTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(duplicate)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void login_wrongPassword_returns401() {
        register(EMAIL, FIRST_NAME, PASSWORD);

        LoginRequestDto request = new LoginRequestDto(EMAIL, PASSWORD + "-wrong");

        restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknownEmail_returns401() {
        LoginRequestDto request = new LoginRequestDto("unknown@example.com", PASSWORD);

        restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedEndpoint_withoutAccessToken_returns403() {
        restTestClient.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void protectedEndpoint_withInvalidAccessToken_returns401() {
        restTestClient.get()
                .uri("/api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refresh_withoutCookie_returns401() {
        restTestClient.post()
                .uri("/api/auth/refresh")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refresh_withInvalidToken_returns401() {
        restTestClient.post()
                .uri("/api/auth/refresh")
                .cookie("refreshToken", "invalid-refresh-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refresh_whenTokenMissingInRedis_returns401() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String refreshToken = loginAndGetRefreshToken(EMAIL, PASSWORD);

        var keys = redisTemplate.keys("refresh:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        restTestClient.post()
                .uri("/api/auth/refresh")
                .cookie("refreshToken", refreshToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void refresh_reusedOldToken_returns401() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String oldRefreshToken = loginAndGetRefreshToken(EMAIL, PASSWORD);

        restTestClient.post()
                .uri("/api/auth/refresh")
                .cookie("refreshToken", oldRefreshToken)
                .exchange()
                .expectStatus().isOk();

        restTestClient.post()
                .uri("/api/auth/refresh")
                .cookie("refreshToken", oldRefreshToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logout_withoutCookie_returns401() {
        restTestClient.post()
                .uri("/api/auth/logout")
                .exchange()
                .expectStatus().isUnauthorized();
    }

}