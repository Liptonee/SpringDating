package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.petproject.dating_backend.user.PatchRequestDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

class PatchFlowNegativeIT extends BaseIntegrationTest {

    @Test
    void patch_withoutAccessToken_returns403() {
        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(emptyPatch())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void patch_withInvalidAccessToken_returns401() {
        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .body(emptyPatch())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void patch_withDuplicateEmail_returns409() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        register("other@example.com", FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        PatchRequestDto request = new PatchRequestDto(
                null, "other@example.com", null, null, null, null, null, null, null);

        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @ParameterizedTest(name = "{0} -> returns 400")
    @MethodSource("invalidPatchRequests")
    void patch_withInvalidData_returns400(String description, PatchRequestDto invalidRequest) {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(invalidRequest)
                .exchange()
                .expectStatus().isBadRequest();
    }

    static Stream<Arguments> invalidPatchRequests() {
        return Stream.of(
                Arguments.of("invalid email format", new PatchRequestDto(null, "not-an-email", null, null, null, null, null, null, null)),
                Arguments.of("too short first name", new PatchRequestDto("A", null, null, null, null, null, null, null, null)),
                Arguments.of("too long first name", new PatchRequestDto("a".repeat(31), null, null, null, null, null, null, null, null)),
                Arguments.of("age below min (17)", new PatchRequestDto(null, null, null, null, (short) 17, null, null, null, null)),
                Arguments.of("age above max (101)", new PatchRequestDto(null, null, null, null, (short) 101, null, null, null, null)),
                Arguments.of("too short city", new PatchRequestDto(null, null, null, null, null, "A", null, null, null)),
                Arguments.of("too long short about", new PatchRequestDto(null, null, "a".repeat(128), null, null, null, null, null, null)),
                Arguments.of("too long full about", new PatchRequestDto(null, null, null, "a".repeat(513), null, null, null, null, null))
        );
    }

    @Test
    void patch_withPreferredAgeMinGreaterThanMax_returns400() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        PatchRequestDto request = new PatchRequestDto(
                null, null, null, null, null, null, null,
                (short) 40, (short) 20);

        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void patch_withPreferredAgeMinGreaterThanExistingMax_returns400() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        PatchRequestDto first = new PatchRequestDto(
                null, null, null, null, null, null, null, null, (short) 30);
        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(first)
                .exchange()
                .expectStatus().isOk();

        PatchRequestDto second = new PatchRequestDto(
                null, null, null, null, null, null, null, (short) 40, null);
        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(second)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getCurrentUser_withoutAccessToken_returns403() {
        restTestClient.get()
                .uri("/api/users/me")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getCompleteness_withoutAccessToken_returns403() {
        restTestClient.get()
                .uri("/api/users/me/completeness")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getDeck_whenProfileIncomplete_returns403() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        restTestClient.get()
                .uri("/api/deck")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void getUser_withUnknownId_returns404() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        restTestClient.get()
                .uri("/api/users/999999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    private PatchRequestDto emptyPatch() {
        return new PatchRequestDto(null, null, null, null, null, null, null, null, null);
    }
}