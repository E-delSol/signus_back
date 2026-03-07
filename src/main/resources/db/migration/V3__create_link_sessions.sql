CREATE TABLE link_sessions (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);
