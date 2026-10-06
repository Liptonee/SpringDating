package org.petproject.dating_backend.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(
                        name = "idx_chat_messages_room_created",
                        columnList = "room_id, created_at DESC"
                ),
        }
)
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

}