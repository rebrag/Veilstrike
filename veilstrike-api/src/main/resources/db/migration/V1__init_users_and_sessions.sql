CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    google_id     VARCHAR(255) UNIQUE,
    auth_provider VARCHAR(10)  NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_auth_provider CHECK (
        (auth_provider = 'LOCAL'  AND password_hash IS NOT NULL)
        OR
        (auth_provider = 'GOOGLE' AND google_id IS NOT NULL)
    )
);

CREATE TABLE sessions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token_hash VARCHAR(255) NOT NULL,
    expires_at         TIMESTAMPTZ  NOT NULL,
    revoked            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_agent         VARCHAR(512),
    ip_address         VARCHAR(45)
);

CREATE INDEX idx_sessions_user_id            ON sessions(user_id);
CREATE INDEX idx_sessions_refresh_token_hash ON sessions(refresh_token_hash);
