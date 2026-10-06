CREATE TABLE chat_messages
(
    id         BIGSERIAL PRIMARY KEY,
    room_id    BIGINT      NOT NULL,
    sender_id  BIGINT      NOT NULL,
    content    TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_read    BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_messages_room_created ON chat_messages (room_id, created_at DESC);
CREATE INDEX idx_chat_messages_room_unread ON chat_messages (room_id, sender_id) WHERE is_read = FALSE;