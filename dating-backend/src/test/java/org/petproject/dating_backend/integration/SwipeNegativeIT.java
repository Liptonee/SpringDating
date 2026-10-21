package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRequestDto;
import org.petproject.dating_backend.user.PatchRequestDto;
import org.petproject.dating_backend.user.ProfileUserDto;
import org.petproject.dating_backend.user.UserGender;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

class SwipeNegativeIT extends BaseIntegrationTest {

    @Test
    void swipe_self_returns400_and_noSideEffects() {
        Long userId = registerAndGetId("self@mail.com", "Self", PASSWORD);
        String token = loginAndGetAccessToken("self@mail.com", PASSWORD);
        completeUser(token);
        saveMockPhoto(userId);

        restTestClient.post()
                .uri("/api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new SwipeRequestDto(userId, SwipeAction.LIKE))
                .exchange()
                .expectStatus().isBadRequest();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM swipes", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM matches", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_rooms", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class)).isZero();
    }

    @Test
    void swipe_nonExistentUser_returns404_and_noSideEffects() {
        Long userId = registerAndGetId("user1@mail.com", "User1", PASSWORD);
        String token = loginAndGetAccessToken("user1@mail.com", PASSWORD);
        completeUser(token);
        saveMockPhoto(userId);

        restTestClient.post()
                .uri("/api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new SwipeRequestDto(9999L, SwipeAction.LIKE))
                .exchange()
                .expectStatus().isNotFound();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM swipes", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM matches", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_rooms", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class)).isZero();
    }

    @Test
    void swipe_withUnfilledProfile_returns403_and_noSideEffects() {
        register("unfilled@mail.com", "Unfilled", PASSWORD);
        String user1Token = loginAndGetAccessToken("unfilled@mail.com", PASSWORD);
        // user1 has no profile patch and no photo

        Long user2Id = registerAndGetId("target@mail.com", "Target", PASSWORD);
        // user2 exists

        restTestClient.post()
                .uri("/api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + user1Token)
                .body(new SwipeRequestDto(user2Id, SwipeAction.LIKE))
                .exchange()
                .expectStatus().isForbidden();

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM swipes", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM matches", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_rooms", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications", Integer.class)).isZero();
    }

}