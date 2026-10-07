package org.petproject.dating_backend.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserService userService;
    private final ChatMessageMapper messageMapper;
    private final ChatRoomMapper roomMapper;

    private final ApplicationEventPublisher eventPublisher;

    private static final int PAGE_SIZE = 50;

    @Transactional
    public ChatMessageResponseDto sendMessage(Long curUserId, Long roomId, ChatMessageRequestDto request) {
        ChatRoomEntity roomEntity = roomRepository.findById(roomId).orElseThrow(
                () -> new NotFoundException("Комната с таким id не существует", 404)
        );
        if (!roomRepository.hasMemberById(roomId, curUserId)) {
            throw new ForbiddenException("Пользователь не является участником комнаты (чата)", 403);
        }

        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(request.content());
        messageEntity.setRoomId(roomId);
        messageEntity.setSenderId(curUserId);
        messageEntity.setCreatedAt(Instant.now());
        messageRepository.save(messageEntity);


        Long receiverId = roomEntity.getFirstUserId().equals(curUserId)
                ? roomEntity.getSecondUserId()
                : roomEntity.getFirstUserId();

        eventPublisher.publishEvent(new ChatMessageEvent(
                messageEntity.getId(),
                curUserId,
                receiverId,
                messageEntity.getContent()
        ));


        return messageMapper.toDto(messageEntity, userService);
    }

    @Transactional
    public void markRead(Long curUserId, Long roomId) {
        if (!roomRepository.hasMemberById(roomId, curUserId)) {
            throw new ForbiddenException("Пользователь не является участником комнаты (чата)", 403);
        }

        messageRepository.markAllReadInRoom(roomId, curUserId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponseDto> getMessages(Long curUserId, Long roomId, Long beforeId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("Комната с таким id не существует", 404);
        }
        if (!roomRepository.hasMemberById(roomId, curUserId)) {
            throw new ForbiddenException("Пользователь не является участником комнаты (чата)", 403);
        }

        Pageable pageable = PageRequest.of(0, PAGE_SIZE);
        List<ChatMessageEntity> messages =
                messageRepository.findPage(roomId, beforeId, pageable);

        List<ChatMessageResponseDto> dtos = messages.stream()
                .map(m -> messageMapper.toDto(m, userService))
                .toList();

        return dtos.reversed();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createRoom(MatchCreatedEvent event) {
        ChatRoomEntity roomEntity = roomRepository.createBetween(event.firstUserId(), event.secondUserId());
        try{
            roomRepository.save(roomEntity);
        } catch (Exception e) {
            log.error("Лайк+мэтч успешно созданы (коммит), но команту-чат создать не удалось." +
                            "matchId - {}, firstUserId - {}, secondUserId - {}",
                    event.matchId(),
                    event.firstUserId(),
                    event.secondUserId());
        }
    }

    public Page<ChatRoomResponseDto> getRooms(Long curUserId, Pageable pageable) {

        return roomRepository.findAllByUserId(curUserId, pageable)
                .map(entity -> roomMapper.toResponseDto(entity, curUserId));

    }
}
