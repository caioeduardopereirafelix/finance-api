CREATE TABLE bank_connections (
    id               UUID         NOT NULL,
    user_id          UUID         NOT NULL,
    provider         VARCHAR(30)  NOT NULL,
    external_id      VARCHAR(120) NOT NULL,
    institution_name VARCHAR(120),
    status           VARCHAR(20)  NOT NULL,
    last_synced_at   TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_bank_connections PRIMARY KEY (id),
    CONSTRAINT fk_bank_connections_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_bank_connections_provider_external UNIQUE (provider, external_id)
);

CREATE INDEX idx_bank_connections_user ON bank_connections (user_id);

ALTER TABLE transactions
    ADD COLUMN source             VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN external_id        VARCHAR(160),
    ADD COLUMN occurred_at        TIMESTAMP WITH TIME ZONE,
    ADD COLUMN bank_connection_id UUID;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_bank_connection
        FOREIGN KEY (bank_connection_id) REFERENCES bank_connections (id) ON DELETE SET NULL;

-- Impede importar duas vezes a mesma transacao do banco. Parcial de proposito:
-- lancamentos manuais nao tem external_id e nao entram na regra.
CREATE UNIQUE INDEX uk_transactions_user_external
    ON transactions (user_id, external_id)
    WHERE external_id IS NOT NULL;
