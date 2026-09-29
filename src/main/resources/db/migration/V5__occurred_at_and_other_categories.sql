-- Data do gasto passa a ser obrigatoria e a valer para listar/ordenar/filtrar.
-- Lancamentos anteriores usam o momento em que foram registrados, que era a
-- unica data que existia para eles.
UPDATE transactions SET occurred_at = created_date WHERE occurred_at IS NULL;
UPDATE transactions SET occurred_at = NOW()        WHERE occurred_at IS NULL;

ALTER TABLE transactions ALTER COLUMN occurred_at SET NOT NULL;

-- A listagem agora ordena por occurred_at; o indice antigo (created_date) deixa de servir.
DROP INDEX IF EXISTS idx_transactions_user_created_date;
CREATE INDEX idx_transactions_user_occurred_at ON transactions (user_id, occurred_at DESC);

-- As categorias OTHER_INCOME e OTHER_EXPENSE nao precisam de migration:
-- transactions.category e VARCHAR sem restricao.
