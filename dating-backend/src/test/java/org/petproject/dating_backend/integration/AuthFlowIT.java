package org.petproject.dating_backend.integration;


import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.auth.AuthResponseDto;
import org.petproject.dating_backend.auth.LoginRequestDto;
import org.petproject.dating_backend.auth.RegisterRequestDto;
import org.petproject.dating_backend.user.ProfileUserDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIT extends BaseIntegrationTest {


    @Test
    void shouldCorrectProcessFullAuthFlow() {
        //register
        RegisterRequestDto regReq = new RegisterRequestDto(EMAIL, FIRST_NAME, PASSWORD);

        ProfileUserDto regResponse = restTestClient.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(regReq)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProfileUserDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(regResponse).isNotNull();
        assertThat(regResponse)
                .extracting(ProfileUserDto::email, ProfileUserDto::firstName)
                .containsExactly(EMAIL.toLowerCase().trim(), FIRST_NAME);

        //login
        LoginRequestDto logReq = new LoginRequestDto(EMAIL, PASSWORD);


        var logResult = restTestClient.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(logReq)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectHeader().valueMatches(HttpHeaders.SET_COOKIE, "refreshToken=.+")
                .expectBody(AuthResponseDto.class)
                .returnResult();
        String refreshToken = logResult.getResponseCookies().getFirst("refreshToken").getValue();
        AuthResponseDto logResponse = logResult.getResponseBody();

        assertThat(refreshToken).isNotBlank();
        assertThat(logResponse).isNotNull();
        assertThat(logResponse.token()).isNotBlank();
        assertThat(redisTemplate.keys("refresh:*")).hasSize(1);
        //access token
        String accessToken = logResponse.token();

        //protected - completeness
        Set<String> notReadyFields = restTestClient.get()
                .uri("/api/users/me/completeness")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Set<String>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(notReadyFields).isEqualTo(Set.of(
                "gender", "age", "city", "short about",
                "main photo", "preferred age max", "preferred age min"
        ));

        //refresh
        var refreshResult = restTestClient.post()
                .uri("/api/auth/refresh")
                .cookie("refreshToken", refreshToken)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .expectHeader().valueMatches(HttpHeaders.SET_COOKIE, "refreshToken=.+")
                .expectBody(AuthResponseDto.class)
                .returnResult();
        String newRefreshToken = refreshResult.getResponseCookies().getFirst("refreshToken").getValue();

        assertThat(newRefreshToken).isNotBlank().isNotEqualTo(refreshToken);

        //logout
        var logoutResult = restTestClient.post()
                .uri("/api/auth/logout")
                .cookie("refreshToken", newRefreshToken)
                .exchange()
                .expectStatus().isNoContent()
                .expectHeader().exists(HttpHeaders.SET_COOKIE)
                .returnResult();
        var cookie = logoutResult.getResponseCookies().getFirst("refreshToken");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.isHttpOnly()).isTrue();
    }
}