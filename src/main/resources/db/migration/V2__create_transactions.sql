CREATE TABLE transactions (
    id                 UUID           NOT NULL,
    description        VARCHAR(255),
    amount             NUMERIC(38, 2),
    type               VARCHAR(50),
    category           VARCHAR(50),
    user_id            UUID,
    created_by         VARCHAR(100),
    created_date       TIMESTAMP WITH TIME ZONE,
    last_modified_by   VARCHAR(100),
    last_modified_date TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_transactions_user_created_date
    ON transactions (user_id, created_date DESC);
