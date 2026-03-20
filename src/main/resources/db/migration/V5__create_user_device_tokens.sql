CREATE TABLE user_device_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(191) NOT NULL,
    fcm_token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    app_version VARCHAR(64),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    last_registered_at BIGINT NOT NULL,
    deactivated_at BIGINT
);

CREATE UNIQUE INDEX ux_user_device_tokens_user_device
    ON user_device_tokens (user_id, device_id);

CREATE UNIQUE INDEX ux_user_device_tokens_active_fcm_token
    ON user_device_tokens (fcm_token)
    WHERE active = TRUE;

CREATE INDEX idx_user_device_tokens_user_active
    ON user_device_tokens (user_id, active);

CREATE INDEX idx_user_device_tokens_last_registered_at
    ON user_device_tokens (last_registered_at DESC);

INSERT INTO user_device_tokens (
    id,
    user_id,
    device_id,
    fcm_token,
    platform,
    app_version,
    active,
    created_at,
    updated_at,
    last_registered_at,
    deactivated_at
)
SELECT
    id,
    id,
    'legacy-primary',
    fcm_token,
    'android',
    NULL,
    TRUE,
    created_at,
    created_at,
    created_at,
    NULL
FROM users
WHERE fcm_token IS NOT NULL
  AND LENGTH(TRIM(fcm_token)) > 0
ON CONFLICT (user_id, device_id) DO NOTHING;
