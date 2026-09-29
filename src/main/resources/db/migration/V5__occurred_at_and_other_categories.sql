
UPDATE transactions SET occurred_at = created_date WHERE occurred_at IS NULL;
UPDATE transactions SET occurred_at = NOW()        WHERE occurred_at IS NULL;

ALTER TABLE transactions ALTER COLUMN occurred_at SET NOT NULL;

DROP INDEX IF EXISTS idx_transactions_user_created_date;
CREATE INDEX idx_transactions_user_occurred_at ON transactions (user_id, occurred_at DESC);

