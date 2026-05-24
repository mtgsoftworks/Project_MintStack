-- Market-data reset deactivates real instruments. Keep the BIST stock catalog
-- recoverable so the scheduler can fetch fresh prices after a reset.
UPDATE instruments
SET is_active = TRUE,
    is_simulated = FALSE,
    exchange = COALESCE(NULLIF(exchange, ''), 'BIST'),
    currency = COALESCE(NULLIF(currency, ''), 'TRY'),
    updated_at = CURRENT_TIMESTAMP
WHERE type = 'STOCK'
  AND (is_simulated IS NULL OR is_simulated = FALSE)
  AND current_price IS NOT NULL;
