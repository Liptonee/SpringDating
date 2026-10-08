ALTER TABLE chat_rooms
    ADD COLUMN last_message_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX idx_chat_rooms_first_last
    ON chat_rooms (first_user_id, last_message_at DESC);

CREATE INDEX idx_chat_rooms_second_last
    ON chat_rooms (second_user_id, last_message_at DESC);