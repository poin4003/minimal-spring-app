ALTER TABLE media
    ADD COLUMN thumbnail_storage_key VARCHAR(255);

CREATE UNIQUE INDEX uk_media_thumbnail_storage_key
    ON media(thumbnail_storage_key);
