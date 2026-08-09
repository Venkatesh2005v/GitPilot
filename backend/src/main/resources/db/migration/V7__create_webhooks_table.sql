CREATE TABLE IF NOT EXISTS repository_webhook (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repository(id) ON DELETE CASCADE,
    github_webhook_id BIGINT NOT NULL,
    payload_url VARCHAR(512) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    events VARCHAR(255) NOT NULL DEFAULT 'push',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_delivery_at TIMESTAMP,
    UNIQUE(repository_id)
);

CREATE INDEX idx_repo_webhook_repo_id ON repository_webhook(repository_id);
