package org.petproject.dating_backend.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.petproject.dating_backend.chat.ChatMessageRequestDto;
import org.petproject.dating_backend.chat.ChatMessageResponseDto;
import org.petproject.dating_backend.chat.ChatRoomResponseDto;
import org.petproject.dating_backend.chat.MarkReadResponseDto;
import org.petproject.dating_backend.common.dto.PageResponse;
import org.petproject.dating_backend.common.exception.ErrorDto;
import org.petproject.dating_backend.swipe.SwipeAction;
import org.petproject.dating_backend.swipe.SwipeRequestDto;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class ChatWebSocketFlowIT extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    private final List<WebSocketStompClient> clients = new ArrayList<>();
    private final List<StompSession> sessions = new ArrayList<>();

    @AfterEach
    void closeWebSockets() {
        sessions.forEach(s -> {
            if (s.isConnected()) {
                try {
                    s.disconnect();
                } catch (Exception ignored) {
                }
            }
        });
        clients.forEach(WebSocketStompClient::stop);
        sessions.clear();
        clients.clear();
    }


    private StompSession openSession(String token) throws Exception {
        WebSocketStompClient client = StompTestHelper.client();
        StompSession session = StompTestHelper.connect(client, port, token);
        clients.add(client);
        sessions.add(session);
        return session;
    }

    private void swipe(String token, Long toId, SwipeAction action) {
        restTestClient.post()
                .uri("/api/swipes")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(new SwipeRequestDto(toId, action))
                .exchange()
                .expectStatus().isCreated();
    }

    private record MatchedPair(Long firstUserId, String firstToken,
                               Long secondUserId, String secondToken,
                               Long roomId) {
    }

    private MatchedPair createMatchedPair(String prefix) {
        Long firstUserId = registerAndGetId(prefix + ".first@example.com", "First", PASSWORD);
        String firstToken = loginAndGetAccessToken(prefix + ".first@example.com", PASSWORD);
        completeUser(firstToken);
        saveMockPhoto(firstUserId);

        Long secondUserId = registerAndGetId(prefix + ".second@example.com", "Second", PASSWORD);
        String secondToken = loginAndGetAccessToken(prefix + ".second@example.com", PASSWORD);
        completeUser(secondToken);
        saveMockPhoto(secondUserId);

        swipe(firstToken, secondUserId, SwipeAction.LIKE);
        swipe(secondToken, firstUserId, SwipeAction.LIKE);

        // ИСПРАВЛЕННЫЙ БЛОК:
        Long roomId = restTestClient.get()
                .uri("/api/chats")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<ChatRoomResponseDto>>() {
                })
                .returnResult()
                .getResponseBody()
                .getContent()
                .stream()
                .findFirst()
                .orElseThrow()
                .roomId();

        return new MatchedPair(firstUserId, firstToken, secondUserId, secondToken, roomId);
    }


    @Test
    void shouldDeliverMessageToRoomSubscriber() throws Exception {
        MatchedPair pair = createMatchedPair("deliver");
        String content = "Hello via WebSocket!";

        StompSession senderSession = openSession(pair.firstToken());
        StompSession receiverSession = openSession(pair.secondToken());

        BlockingQueue<ChatMessageResponseDto> inbox = StompTestHelper.subscribe(
                receiverSession,
                "/topic/chat/room/" + pair.roomId(),
                ChatMessageResponseDto.class
        );

        // When
        senderSession.send(
                "/app/chat/room/" + pair.roomId() + "/sendMessage",
                new ChatMessageRequestDto(content)
        );

        // Then
        ChatMessageResponseDto msg = inbox.poll(5, TimeUnit.SECONDS);
        assertThat(msg).isNotNull();
        assertThat(msg.id()).isNotNull();
        assertThat(msg.roomId()).isEqualTo(pair.roomId());
        assertThat(msg.senderId()).isEqualTo(pair.firstUserId());
        assertThat(msg.content()).isEqualTo(content);
        assertThat(msg.senderName()).isEqualTo("First");
    }

    @Test
    void shouldBroadcastReadReceiptToRoomSubscribers() throws Exception {
        MatchedPair pair = createMatchedPair("read");

        StompSession senderSession = openSession(pair.firstToken());
        senderSession.send(
                "/app/chat/room/" + pair.roomId() + "/sendMessage",
                new ChatMessageRequestDto("Please mark me as read")
        );

        StompSession readerSession = openSession(pair.secondToken());
        BlockingQueue<MarkReadResponseDto> receipts = StompTestHelper.subscribe(
                readerSession,
                "/topic/chat/room/" + pair.roomId() + "/read",
                MarkReadResponseDto.class
        );

        // When
        readerSession.send("/app/chat/room/" + pair.roomId() + "/markRead", null);

        // Then
        MarkReadResponseDto receipt = receipts.poll(5, TimeUnit.SECONDS);
        assertThat(receipt).isNotNull();
        assertThat(receipt.roomId()).isEqualTo(pair.roomId());
        assertThat(receipt.readerId()).isEqualTo(pair.secondUserId());
        assertThat(receipt.createdAt()).isNotNull();
    }

    @Test
    void shouldReturnErrorWhenNonMemberSendsMessage() throws Exception {
        MatchedPair pair = createMatchedPair("err");

        Long strangerId = registerAndGetId("stranger@example.com", "Stranger", PASSWORD);
        String strangerToken = loginAndGetAccessToken("stranger@example.com", PASSWORD);
        completeUser(strangerToken);
        saveMockPhoto(strangerId);

        StompSession strangerSession = openSession(strangerToken);

        BlockingQueue<ErrorDto> errors = StompTestHelper.subscribe(
                strangerSession,
                "/user/queue/errors",
                ErrorDto.class
        );

        strangerSession.send(
                "/app/chat/room/" + pair.roomId() + "/sendMessage",
                new ChatMessageRequestDto("Intrusion attempt")
        );

        ErrorDto error = errors.poll(5, TimeUnit.SECONDS);
        assertThat(error).isNotNull();
        assertThat(error.title()).isEqualTo("Chat error");
        assertThat(error.details()).contains("не является участником");
    }

    @Test
    void shouldNotReceiveMessagesFromForeignRoom() throws Exception {
        MatchedPair pair = createMatchedPair("foreign");
        Long strangerId = registerAndGetId("outsider@example.com", "Outsider", PASSWORD);
        String strangerToken = loginAndGetAccessToken("outsider@example.com", PASSWORD);
        completeUser(strangerToken);
        saveMockPhoto(strangerId);

        StompSession senderSession = openSession(pair.firstToken());
        StompSession strangerSession = openSession(strangerToken);

        BlockingQueue<ChatMessageResponseDto> strangerInbox = StompTestHelper.subscribe(
                strangerSession,
                "/user/queue/messages", // заведомо неиспользуемый destination — просто чтобы «включить» сессию
                ChatMessageResponseDto.class
        );

        senderSession.send(
                "/app/chat/room/" + pair.roomId() + "/sendMessage",
                new ChatMessageRequestDto("Only for room members")
        );

        assertThat(strangerInbox.poll(1, TimeUnit.SECONDS)).isNull();
    }
}
