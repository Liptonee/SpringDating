package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.chat.ChatRoomRepository;
import org.petproject.dating_backend.match.MatchRepository;
import org.petproject.dating_backend.swipe.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class UndoLikeFlowIT extends BaseIntegrationTest {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;
    @Autowired
    private SwipeRepository swipeRepository;

    @Test
    void shouldDeleteMatchAndRoom_whenUndoLike() {
        Long firstUserId = registerAndGetId("firstIvan@mail.com", "Ivan", PASSWORD);
        String firstToken = loginAndGetAccessToken("firstIvan@mail.com", PASSWORD);
        completeUser(firstToken);
        saveMockPhoto(firstUserId);
        Long secondUserId = registerAndGetId("secondVika@mail.com", "Vika", PASSWORD);
        String secondToken = loginAndGetAccessToken("secondVika@mail.com", PASSWORD);
        completeUser(secondToken);
        saveMockPhoto(secondUserId);

        //from first to second LIKE
        restTestClient.post()
                .uri("api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                .body(new SwipeRequestDto(secondUserId, SwipeAction.LIKE))
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody(SwipeResponseDto.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody().isMatched()).isFalse();
                });


        //from second to first LIKE
        restTestClient.post()
                .uri("api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                .body(new SwipeRequestDto(firstUserId, SwipeAction.LIKE))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SwipeResponseDto.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody().isMatched()).isTrue();
                });


        //undo like from second
        restTestClient.delete()
                .uri("/api/swipes/2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                .exchange()
                .expectStatus().isNoContent();

        assertThat(swipeRepository.findAll()).hasSize(1)
                .extracting(SwipeEntity::getFromId, SwipeEntity::getToId)
                .containsExactly(tuple(firstUserId, secondUserId));
        assertThat(matchRepository.findAll()).isEmpty();
        assertThat(chatRoomRepository.findAll()).isEmpty();
    }


}
