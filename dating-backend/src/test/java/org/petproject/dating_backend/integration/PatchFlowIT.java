package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.deck.CardDto;
import org.petproject.dating_backend.photo.PhotoEntity;
import org.petproject.dating_backend.photo.PhotoRepository;
import org.petproject.dating_backend.user.PatchRequestDto;
import org.petproject.dating_backend.user.ProfileUserDto;
import org.petproject.dating_backend.user.UserGender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatchFlowIT extends BaseIntegrationTest {

    @Autowired
    private PhotoRepository photoRepository;
    @Autowired
    private CacheManager cacheManager;

    @Test
    void shouldCorrectProcess_whenCorrectInputPatch() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String accessToken = loginAndGetAccessToken(EMAIL, PASSWORD);

        Set<String> fullyNotReady = restTestClient.get()
                .uri("/api/users/me/completeness")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Set<String>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(fullyNotReady).isEqualTo(Set.of(
                "gender", "age", "city", "short about",
                "main photo", "preferred age max", "preferred age min"
        ));


        PatchRequestDto patchReq = new PatchRequestDto(null, null,
                "шорт", "фул",
                (short) 25, "город",
                UserGender.MALE, (short) 20, (short) 30);

        restTestClient.patch()
                .uri("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .body(patchReq)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProfileUserDto.class);

        Set<String> photoNotReady = restTestClient.get()
                .uri("/api/users/me/completeness")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Set<String>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(photoNotReady).isEqualTo(Set.of("main photo"));

        restTestClient.get()
                .uri("/api/deck")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isForbidden();

        saveMockPhoto(1L);

        Set<String> ready = restTestClient.get()
                .uri("/api/users/me/completeness")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Set<String>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(ready).isEmpty();

        List<CardDto> deck = restTestClient.get()
                .uri("/api/deck")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<CardDto>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(deck).isNotNull().isEmpty();
    }


    void saveMockPhoto(Long userId) {
        PhotoEntity photo = new PhotoEntity();
        photo.setContentType("mock");
        photo.setMain(true);
        photo.setObjectName("mock");
        photo.setOriginalName("mock");
        photo.setUserId(userId);
        photo.setSize(10L);
        photoRepository.save(photo);
        cacheManager.getCache("photos").clear();
    }
}
