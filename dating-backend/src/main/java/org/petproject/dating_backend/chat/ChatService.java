package org.petproject.dating_backend.chat;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.match.MatchCreatedEvent;
import org.petproject.dating_backend.user.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.sound.sampled.AudioFormat;
import javax.swing.text.html.parser.Entity;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserService userService;
    private final ChatMessageMapper messageMapper;

    private final ApplicationEventPublisher eventPublisher;

    private static final int PAGE_SIZE = 50;

    @Transactional
    public ChatMessageResponseDto sendMessage(Long curUserId, ChatMessageRequestDto request) {
        ChatRoomEntity roomEntity = roomRepository.findById(request.roomId()).orElseThrow(
                () -> new NotFoundException("Комната с таким id не существует", 404)
        );
        if (!roomRepository.hasMemberById(request.roomId(), curUserId)) {
            throw new ForbiddenException("Пользователь не является участником комнаты (чата)", 403);
        }

        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(request.content());
        messageEntity.setRoomId(request.roomId());
        messageEntity.setSenderId(curUserId);
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


        return messageMapper.toDto(messageEntity, curUserId, userService);
    }

    @Transactional
    public void markRead(Long curUserId, Long roomId) {
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
                .map(m -> messageMapper.toDto(m, curUserId, userService))
                .toList();

        return dtos.reversed();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void createRoom(MatchCreatedEvent event) {
        ChatRoomEntity roomEntity = roomRepository.createBetween(event.firstUserId(), event.secondUserId());
        roomRepository.save(roomEntity);
    }

}
