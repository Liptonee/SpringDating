package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeEntity;
import org.petproject.dating_backend.swipe.SwipeRepository;
import org.petproject.dating_backend.swipe.SwipeRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class UndoSwipeNegativeIT extends BaseIntegrationTest {

    @Autowired
    private SwipeRepository swipeRepository;

    @Test
    void undo_swipeNotFound_returns404() {
        register(EMAIL, FIRST_NAME, PASSWORD);
        String token = loginAndGetAccessToken(EMAIL, PASSWORD);

        restTestClient.delete()
                .uri("/api/swipes/999999")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(swipeRepository.findAll()).isEmpty();
    }

    @Test
    void undo_foreignSwipe_returns403() {
        Long firstUserId = registerAndGetId("firstIvan@mail.com", "Ivan", PASSWORD);
        String firstToken = loginAndGetAccessToken("firstIvan@mail.com", PASSWORD);
        completeUser(firstToken);
        saveMockPhoto(firstUserId);

        Long secondUserId = registerAndGetId("secondVika@mail.com", "Vika", PASSWORD);
        String secondToken = loginAndGetAccessToken("secondVika@mail.com", PASSWORD);
        completeUser(secondToken);
        saveMockPhoto(secondUserId);

        // from first to second LIKE
        restTestClient.post()
                .uri("/api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                .body(new SwipeRequestDto(secondUserId, SwipeAction.LIKE))
                .exchange()
                .expectStatus().isCreated();

        Long swipeId = swipeRepository.findAll().getFirst().getId();

        // second tries to undo first's swipe
        restTestClient.delete()
                .uri("/api/swipes/" + swipeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                .exchange()
                .expectStatus().isForbidden();

        assertThat(swipeRepository.findAll()).hasSize(1)
                .extracting(SwipeEntity::getFromId, SwipeEntity::getToId)
                .containsExactly(tuple(firstUserId, secondUserId));
    }

    @Test
    void undo_withoutAccessToken_returns403() {
        restTestClient.delete()
                .uri("/api/swipes/1")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void undo_withInvalidAccessToken_returns401() {
        restTestClient.delete()
                .uri("/api/swipes/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void undo_alreadyUndoneSwipe_returns404() {
        Long firstUserId = registerAndGetId("firstIvan@mail.com", "Ivan", PASSWORD);
        String firstToken = loginAndGetAccessToken("firstIvan@mail.com", PASSWORD);
        completeUser(firstToken);
        saveMockPhoto(firstUserId);

        Long secondUserId = registerAndGetId("secondVika@mail.com", "Vika", PASSWORD);
        String secondToken = loginAndGetAccessToken("secondVika@mail.com", PASSWORD);
        completeUser(secondToken);
        saveMockPhoto(secondUserId);

        restTestClient.post()
                .uri("/api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                .body(new SwipeRequestDto(secondUserId, SwipeAction.LIKE))
                .exchange()
                .expectStatus().isCreated();

        Long swipeId = swipeRepository.findAll().getFirst().getId();

        restTestClient.delete()
                .uri("/api/swipes/" + swipeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.delete()
                .uri("/api/swipes/" + swipeId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                .exchange()
                .expectStatus().isNotFound();

        assertThat(swipeRepository.findAll()).isEmpty();
    }
}