CREATE TABLE notifications
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    title      VARCHAR(255)  NOT NULL,
    body       VARCHAR(1024) NOT NULL,
    is_read    BOOLEAN       NOT NULL DEFAULT FALSE,
    type       VARCHAR(30)   NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_created
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_notifications_unread
    ON notifications (user_id)
    WHERE is_read = FALSE;