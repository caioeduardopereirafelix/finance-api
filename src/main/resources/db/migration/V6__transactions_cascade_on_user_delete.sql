-- Apagar um usuario falhava (erro 500) quando ele tinha transacoes: esta era a unica chave
-- estrangeira para users sem ON DELETE CASCADE (as de papeis, refresh tokens e conexoes
-- bancarias ja tinham). Apagar a conta passa a apagar tambem o que era dela.
ALTER TABLE transactions DROP CONSTRAINT fk_transactions_user;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;
