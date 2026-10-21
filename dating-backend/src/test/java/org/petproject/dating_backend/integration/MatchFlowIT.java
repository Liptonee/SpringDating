package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.chat.ChatRoomEntity;
import org.petproject.dating_backend.chat.ChatRoomRepository;
import org.petproject.dating_backend.match.MatchEntity;
import org.petproject.dating_backend.match.MatchRepository;
import org.petproject.dating_backend.notification.NotificationEntity;
import org.petproject.dating_backend.notification.NotificationRepository;
import org.petproject.dating_backend.notification.NotificationType;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRequestDto;
import org.petproject.dating_backend.swipe.SwipeResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MatchFlowIT extends BaseIntegrationTest {


    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Test
    void should_createMatch_when_bothUsersLikeEachOther() {
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
                .expectStatus()
                .isCreated()
                .expectBody(SwipeResponseDto.class)
                .consumeWith(result -> {
                    assertThat(result.getResponseBody().isMatched()).isTrue();
                });

        //match created
        assertThat(matchRepository.findAll()).hasSize(1);
        assertThat(matchRepository.findById(1L)).isPresent().get()
                .extracting(MatchEntity::getFirstUserId, MatchEntity::getSecondUserId)
                .containsExactly(1L, 2L);

        //room created
        assertThat(chatRoomRepository.findAll()).hasSize(1);
        assertThat(chatRoomRepository.findById(1L)).isPresent().get()
                .extracting(ChatRoomEntity::getFirstUserId, ChatRoomEntity::getSecondUserId)
                .containsExactly(1L, 2L);

        //all notifications created
        assertThat(notificationRepository.findAll()).hasSize(4)
                .extracting(NotificationEntity::getUserId, NotificationEntity::getType)
                .containsExactlyInAnyOrder(
                        tuple(firstUserId, NotificationType.LIKE),
                        tuple(firstUserId, NotificationType.MATCH),
                        tuple(secondUserId, NotificationType.LIKE),
                        tuple(secondUserId, NotificationType.MATCH)
                );
    }


}
