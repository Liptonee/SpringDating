ALTER TABLE photos ADD COLUMN is_main BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users DROP COLUMN main_photo_id;

CREATE UNIQUE INDEX uq_photos_one_main ON photos(user_id) WHERE is_main;