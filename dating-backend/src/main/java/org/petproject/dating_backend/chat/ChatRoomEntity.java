package org.petproject.dating_backend.chat;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "chat_rooms",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chat_rooms_id_pair",
                columnNames = {"first_user_id", "second_user_id"}
        ),
        indexes = {
                @Index(name = "idx_chat_rooms_first_last", columnList = "first_user_id, last_message_at DESC"),
                @Index(name = "idx_chat_rooms_second_last", columnList = "second_user_id, last_message_at DESC")
        }
)
public class ChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_user_id", nullable = false)
    private Long firstUserId;

    @Column(name = "second_user_id", nullable = false)
    private Long secondUserId;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

}