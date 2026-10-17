package org.petproject.dating_backend.chat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.match.UndoMatchEvent;
import org.petproject.dating_backend.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long ROOM_ID = 10L;
    private static final Long CUR_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long MESSAGE_ID = 555L;

    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private ChatRoomRepository roomRepository;
    @Mock
    private UserService userService;
    @Mock
    private ChatMessageMapper messageMapper;
    @Mock
    private ChatRoomMapper roomMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MeterRegistry registry;
    @Mock
    private Counter counter;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        lenient().when(registry.counter(anyString())).thenReturn(counter);
    }

    private static ChatRoomEntity room() {
        ChatRoomEntity r = new ChatRoomEntity();
        r.setId(ROOM_ID);
        r.setFirstUserId(CUR_USER_ID);
        r.setSecondUserId(OTHER_USER_ID);
        r.setCreatedAt(Instant.now());
        r.setLastMessageAt(Instant.now());
        return r;
    }

    private static ChatMessageRequestDto request() {
        return new ChatMessageRequestDto("Привет!");
    }

    private static ChatMessageEntity message(Long id, Long senderId, String content) {
        ChatMessageEntity m = new ChatMessageEntity();
        m.setId(id);
        m.setRoomId(ROOM_ID);
        m.setSenderId(senderId);
        m.setContent(content);
        m.setCreatedAt(Instant.now());
        return m;
    }

    private static ChatMessageResponseDto messageDto(Long id, Long senderId, String content) {
        return new ChatMessageResponseDto(
                id, ROOM_ID, senderId, "Иван", content, Instant.now(), false
        );
    }


    @Nested
    class SendMessageTest {

        @Test
        void throwsNotFound_whenRoomDoesNotExist() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    chatService.sendMessage(CUR_USER_ID, ROOM_ID, request()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Комната с таким id не существует");

            verify(messageRepository, never()).save(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsForbidden_whenUserIsNotMember() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(false);

            assertThatThrownBy(() ->
                    chatService.sendMessage(CUR_USER_ID, ROOM_ID, request()))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Пользователь не является участником комнаты (чата)");

            verify(messageRepository, never()).save(any());
        }

        @Test
        void savesMessageAndUpdatesLastMessageAt_onSuccess() {
            ChatRoomEntity roomEntity = room();
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(roomEntity));
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);
            when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(inv -> {
                ChatMessageEntity e = inv.getArgument(0);
                e.setId(MESSAGE_ID);
                return e;
            });
            when(userService.getFirstNamesByIds(anyList()))
                    .thenReturn(Map.of(CUR_USER_ID, "Иван"));
            ChatMessageResponseDto expected = messageDto(MESSAGE_ID, CUR_USER_ID, "Привет!");
            when(messageMapper.toDto(any(ChatMessageEntity.class), anyMap()))
                    .thenReturn(expected);

            ChatMessageResponseDto result =
                    chatService.sendMessage(CUR_USER_ID, ROOM_ID, request());

            assertThat(result).isEqualTo(expected);

            ArgumentCaptor<ChatMessageEntity> msgCaptor =
                    ArgumentCaptor.forClass(ChatMessageEntity.class);
            verify(messageRepository).save(msgCaptor.capture());
            ChatMessageEntity saved = msgCaptor.getValue();
            assertThat(saved.getRoomId()).isEqualTo(ROOM_ID);
            assertThat(saved.getSenderId()).isEqualTo(CUR_USER_ID);
            assertThat(saved.getContent()).isEqualTo("Привет!");
            assertThat(saved.getCreatedAt()).isNotNull();

            verify(roomRepository).updateLastMessageAt(ROOM_ID, saved.getCreatedAt());
        }

        @Test
        void publishesEventWithCorrectReceiver_onSuccess() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);
            when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(inv -> {
                ChatMessageEntity e = inv.getArgument(0);
                e.setId(MESSAGE_ID);
                return e;
            });
            when(userService.getFirstNamesByIds(anyList()))
                    .thenReturn(Map.of(CUR_USER_ID, "Иван"));
            when(messageMapper.toDto(any(), anyMap()))
                    .thenReturn(messageDto(MESSAGE_ID, CUR_USER_ID, "Привет!"));

            chatService.sendMessage(CUR_USER_ID, ROOM_ID, request());

            ArgumentCaptor<ChatMessageEvent> captor =
                    ArgumentCaptor.forClass(ChatMessageEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());

            ChatMessageEvent event = captor.getValue();
            assertThat(event.messageId()).isEqualTo(MESSAGE_ID);
            assertThat(event.roomId()).isEqualTo(ROOM_ID);
            assertThat(event.senderId()).isEqualTo(CUR_USER_ID);
            assertThat(event.receiverId()).isEqualTo(OTHER_USER_ID);
            assertThat(event.content()).isEqualTo("Привет!");
        }

        @Test
        void incrementsCounter_onSuccess() {
            when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room()));
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);
            when(messageRepository.save(any(ChatMessageEntity.class))).thenAnswer(inv -> {
                ChatMessageEntity e = inv.getArgument(0);
                e.setId(MESSAGE_ID);
                return e;
            });
            when(userService.getFirstNamesByIds(anyList())).thenReturn(Map.of());
            when(messageMapper.toDto(any(), anyMap())).thenReturn(messageDto(MESSAGE_ID, CUR_USER_ID, "Привет!"));

            chatService.sendMessage(CUR_USER_ID, ROOM_ID, request());

            verify(counter).increment();
        }
    }


    @Nested
    class MarkReadTest {

        @Test
        void throwsForbidden_whenUserIsNotMember() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(false);

            assertThatThrownBy(() -> chatService.markRead(CUR_USER_ID, ROOM_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Пользователь не является участником комнаты (чата)");

            verify(messageRepository, never()).markAllReadInRoom(any(), any());
        }

        @Test
        void marksAllMessagesRead_onSuccess() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);

            chatService.markRead(CUR_USER_ID, ROOM_ID);

            verify(messageRepository).markAllReadInRoom(ROOM_ID, CUR_USER_ID);
        }
    }


    @Nested
    class GetMessagesTest {

        @Test
        void throwsNotFound_whenRoomDoesNotExist() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(false);
            when(roomRepository.existsById(ROOM_ID)).thenReturn(false);

            assertThatThrownBy(() -> chatService.getMessages(CUR_USER_ID, ROOM_ID, null))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Комната с таким id не существует");

            verify(messageRepository, never()).findPage(any(), any(), any());
        }

        @Test
        void throwsForbidden_whenUserIsNotMemberAndRoomExists() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(false);
            when(roomRepository.existsById(ROOM_ID)).thenReturn(true);

            assertThatThrownBy(() -> chatService.getMessages(CUR_USER_ID, ROOM_ID, null))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Пользователь не является участником комнаты (чата)");

            verify(messageRepository, never()).findPage(any(), any(), any());
        }

        @Test
        void queriesWithFixedPageSize_andPassesBeforeId() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);
            when(messageRepository.findPage(eq(ROOM_ID), eq(100L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(userService.getFirstNamesByIds(anySet())).thenReturn(Map.of());

            chatService.getMessages(CUR_USER_ID, ROOM_ID, 100L);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(messageRepository).findPage(eq(ROOM_ID), eq(100L), captor.capture());
            assertThat(captor.getValue().getPageNumber()).isZero();
            assertThat(captor.getValue().getPageSize()).isEqualTo(50);
        }

        @Test
        void returnsDtosInReversedOrder() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);

            ChatMessageEntity newest = message(2L, CUR_USER_ID, "новое");
            ChatMessageEntity oldest = message(1L, OTHER_USER_ID, "старое");
            when(messageRepository.findPage(eq(ROOM_ID), isNull(), any(Pageable.class)))
                    .thenReturn(List.of(newest, oldest));

            Map<Long, String> names = Map.of(CUR_USER_ID, "Иван", OTHER_USER_ID, "Мария");
            when(userService.getFirstNamesByIds(anySet())).thenReturn(names);

            ChatMessageResponseDto dtoNewest = messageDto(2L, CUR_USER_ID, "новое");
            ChatMessageResponseDto dtoOldest = messageDto(1L, OTHER_USER_ID, "старое");
            when(messageMapper.toDto(newest, names)).thenReturn(dtoNewest);
            when(messageMapper.toDto(oldest, names)).thenReturn(dtoOldest);

            List<ChatMessageResponseDto> result =
                    chatService.getMessages(CUR_USER_ID, ROOM_ID, null);


            assertThat(result).containsExactly(dtoOldest, dtoNewest);
        }
    }


    @Nested
    class CreateRoomTest {

        @Test
        void savesRoom_andIncrementsCounter() {
            Instant now = Instant.now();
            MatchCreatedEvent event =
                    new MatchCreatedEvent(1L, CUR_USER_ID, OTHER_USER_ID, now);

            ChatRoomEntity created = room();
            when(roomRepository.createBetween(CUR_USER_ID, OTHER_USER_ID))
                    .thenReturn(created);

            chatService.createRoom(event);

            verify(roomRepository).save(created);
            verify(counter).increment();
        }
    }


    @Nested
    class GetRoomsTest {

        @Test
        void mapsEntitiesToDtos() {
            Pageable pageable = PageRequest.of(0, 20);

            ChatRoomEntity first = room();
            ChatRoomEntity second = room();
            second.setId(11L);

            when(roomRepository.findAllByUserId(CUR_USER_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(first, second)));

            ChatRoomResponseDto dto1 =
                    new ChatRoomResponseDto(first.getId(), OTHER_USER_ID, first.getCreatedAt());
            ChatRoomResponseDto dto2 =
                    new ChatRoomResponseDto(second.getId(), OTHER_USER_ID, second.getCreatedAt());
            when(roomMapper.toResponseDto(first, CUR_USER_ID)).thenReturn(dto1);
            when(roomMapper.toResponseDto(second, CUR_USER_ID)).thenReturn(dto2);

            Page<ChatRoomResponseDto> result = chatService.getRooms(CUR_USER_ID, pageable);

            assertThat(result.getContent()).containsExactly(dto1, dto2);
        }
    }


    @Nested
    class DeleteRoomTest {

        @Test
        void deletesRoomAndIncrementsCounter() {
            UndoMatchEvent event = new UndoMatchEvent(CUR_USER_ID, OTHER_USER_ID);

            chatService.deleteRoom(event);

            verify(roomRepository).deleteBetween(CUR_USER_ID, OTHER_USER_ID);
            verify(counter).increment();
        }
    }


    @Nested
    class IsMemberTest {

        @Test
        void delegatesToRepository() {
            when(roomRepository.hasMemberById(ROOM_ID, CUR_USER_ID)).thenReturn(true);

            boolean result = chatService.isMember(CUR_USER_ID, ROOM_ID);

            assertThat(result).isTrue();
            verify(roomRepository).hasMemberById(ROOM_ID, CUR_USER_ID);
        }
    }
}