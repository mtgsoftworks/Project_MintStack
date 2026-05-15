-- Crypto support is no longer exposed by the application.
-- Keep historical portfolio/transaction rows intact, but remove crypto from active market catalogs.

UPDATE user_data_preferences
SET is_enabled = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE data_type = 'CRYPTO';

UPDATE price_alerts
SET is_active = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE type = 'CRYPTO'
);

DELETE FROM watchlist_items
WHERE instrument_id IN (
    SELECT id FROM instruments WHERE type = 'CRYPTO'
);

UPDATE instruments
SET is_active = FALSE,
    updated_at = CURRENT_TIMESTAMP
WHERE type = 'CRYPTO';
