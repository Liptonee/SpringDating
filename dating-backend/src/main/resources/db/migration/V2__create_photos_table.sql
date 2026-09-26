CREATE TABLE photos (
    id              BIGSERIAL PRIMARY KEY,
    object_name     VARCHAR(255) NOT NULL UNIQUE,
    user_id         BIGINT       NOT NULL,
    original_name   VARCHAR(255) NOT NULL,
    content_type    VARCHAR(255) NOT NULL,
    size            BIGINT       NOT NULL,
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_photos_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_photos_user_id ON photos (user_id);

CREATE INDEX idx_photos_user_uploaded
    ON photos (user_id, uploaded_at DESC);
