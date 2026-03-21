ALTER TABLE link_sessions
    ADD COLUMN IF NOT EXISTS link_code VARCHAR(32);

UPDATE link_sessions
SET link_code = UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 12))
WHERE link_code IS NULL;

ALTER TABLE link_sessions
    ALTER COLUMN link_code SET NOT NULL;

ALTER TABLE link_sessions
    ADD COLUMN IF NOT EXISTS confirmed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS confirmed_by_user_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS ux_link_sessions_link_code ON link_sessions (link_code);
