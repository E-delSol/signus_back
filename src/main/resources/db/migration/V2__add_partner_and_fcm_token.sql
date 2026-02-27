ALTER TABLE users
    ADD COLUMN partner_id VARCHAR(36),
    ADD COLUMN fcm_token VARCHAR(512);
