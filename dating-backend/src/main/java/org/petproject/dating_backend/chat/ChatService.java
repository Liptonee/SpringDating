package org.petproject.dating_backend.chat;

import lombok.RequiredArgsConstructor;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.NotFoundException;
import org.petproject.dating_backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository messageRepository;
    private final ChatRoomRepository roomRepository;
    private final UserService userService;

    @Transactional
    public ChatMessageResponseDto sendMessage(Long curUserId, ChatMessageRequestDto request) {
        if (!roomRepository.existsById(request.roomId())) {
            throw new NotFoundException("Комната с таким id не существует", 404);
        }
        if (!roomRepository.hasMemberById(request.roomId(), curUserId)) {
            throw new ForbiddenException("Пользователь не является участником комнаты (чата)", 403);
        }

        ChatMessageEntity messageEntity = new ChatMessageEntity();
        messageEntity.setContent(request.content());
        messageEntity.setRoomId(request.roomId());
        messageEntity.setSenderId(curUserId);
        messageRepository.save(messageEntity);

        return new ChatMessageResponseDto(
                messageEntity.getId(),
                messageEntity.getRoomId(),
                messageEntity.getSenderId(),
                userService.getFirstName(curUserId),
                messageEntity.getContent(),
                messageEntity.getCreatedAt(),
                messageEntity.isRead()
        );
    }

}
