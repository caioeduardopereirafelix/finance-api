INSERT INTO transactions (id, description, amount, type, category, user_id, created_date, occurred_at, source)
SELECT
    gen_random_uuid(),
    'Transacao ' || g,
    CASE WHEN t.is_expense THEN round((5 + random() * 495)::numeric, 2) ELSE round((500 + random() * 7500)::numeric, 2) END,
    CASE WHEN t.is_expense THEN 'EXPENSES' ELSE 'CASH_ENTRY' END,
    CASE WHEN t.is_expense
         THEN (ARRAY['FOOD','LEISURE','HOUSING','HEALTH','TRANSPORT','INVESTMENTS','BILLS','OTHER_EXPENSE'])[1 + floor(random() * 8)::int]
         ELSE (ARRAY['WAGE','EXTRA_INCOME','OTHER_INCOME'])[1 + floor(random() * 3)::int] END,
    u.id,
    now(),
    now() - (random() * 1095) * interval '1 day',
    'MANUAL'
FROM users u
JOIN LATERAL generate_series(1, CASE WHEN u.email = 'perf1@test.com' THEN 100000 ELSE 50000 END) AS g ON true
CROSS JOIN LATERAL (SELECT random() < 0.85 AS is_expense) t
WHERE u.email LIKE 'perf%@test.com';

ANALYZE transactions;
