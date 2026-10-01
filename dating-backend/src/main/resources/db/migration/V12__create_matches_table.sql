CREATE TABLE matches
(
    id             BIGSERIAL PRIMARY KEY,
    first_user_id  BIGINT      NOT NULL,
    second_user_id BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_matches_user_pair UNIQUE (first_user_id, second_user_id),
    CONSTRAINT check_first_less_than_second CHECK (first_user_id < second_user_id),

    CONSTRAINT fk_matches_first_user FOREIGN KEY (first_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_matches_second_user FOREIGN KEY (second_user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_matches_second_user_id ON matches (second_user_id)
