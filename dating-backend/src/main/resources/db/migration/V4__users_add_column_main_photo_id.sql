ALTER TABLE users
    ADD COLUMN main_photo_id BIGINT,
    ADD CONSTRAINT fk_users_main_photo
        FOREIGN KEY (main_photo_id)
            REFERENCES photos (id)
            ON DELETE SET NULL;