package org.petproject.dating_backend.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.petproject.dating_backend.chat.ChatService;
import org.petproject.dating_backend.common.exception.ForbiddenException;
import org.petproject.dating_backend.common.exception.UnauthorizedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Нет токена в CONNECT", 401);
        }
        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        accessor.setUser(auth);
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw new UnauthorizedException("Не аутентифицирован", 401);
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/user/")) {
            return;
        }

        Long roomId = extractRoomId(destination);
        if (roomId != null) {
            Long userId = Long.valueOf(user.getName());
            if (!chatService.isMember(userId, roomId)) {
                throw new ForbiddenException("Нет доступа к комнате " + roomId, 403);
            }
        }
    }

    /**
     * Извлекает roomId из destination вида:
     *   /topic/chat/room/{roomId}
     *   /topic/chat/room/{roomId}/read
     * Возвращает null, если destination не про чат-комнату.
     */
    private Long extractRoomId(String destination) {
        String prefix = "/topic/chat/room/";
        if (!destination.startsWith(prefix)) {
            return null;
        }
        String rest = destination.substring(prefix.length());

        int slash = rest.indexOf('/');
        String idPart = (slash == -1) ? rest : rest.substring(0, slash);
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            log.warn("Некорректный roomId в destination: {}", destination);
            return null;
        }
    }
}
