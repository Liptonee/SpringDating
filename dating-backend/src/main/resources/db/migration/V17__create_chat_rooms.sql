CREATE TABLE chat_rooms
(
    id             BIGSERIAL PRIMARY KEY,
    first_user_id  BIGINT      NOT NULL,
    second_user_id BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_chat_rooms_id_pair UNIQUE (first_user_id, second_user_id),
    CONSTRAINT check_chat_rooms_id_order CHECK (first_user_id < second_user_id),
    CONSTRAINT fk_first_user_id FOREIGN KEY (first_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_second_user_id FOREIGN KEY (second_user_id) REFERENCES users (id) ON DELETE CASCADE
)
