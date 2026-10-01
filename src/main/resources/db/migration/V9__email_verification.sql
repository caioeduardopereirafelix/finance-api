ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE;

UPDATE users SET email_verified_at = CURRENT_TIMESTAMP;

CREATE TABLE email_verification_tokens (
    id         UUID         NOT NULL,
    token_hash VARCHAR(64)  NOT NULL,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_email_verification_tokens PRIMARY KEY (id),
    CONSTRAINT uk_email_verification_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verification_tokens_user ON email_verification_tokens (user_id);
