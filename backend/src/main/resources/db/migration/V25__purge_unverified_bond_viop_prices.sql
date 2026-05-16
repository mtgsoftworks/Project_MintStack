-- Remove unverified seeded/generated market prices from instruments that do not
-- have an integrated external market-data provider yet.
--
-- BOND and VIOP catalogs stay visible for search/watchlist/alert selection, but
-- price, volume and change values must not look real until a verified provider
-- is connected.

DELETE FROM price_history
WHERE instrument_id IN (
    SELECT id
    FROM instruments
    WHERE type IN ('BOND', 'VIOP')
      AND COALESCE(is_simulated, FALSE) = FALSE
);

UPDATE instruments
SET current_price = NULL,
    previous_close = NULL,
    updated_at = CURRENT_TIMESTAMP
WHERE type IN ('BOND', 'VIOP')
  AND COALESCE(is_simulated, FALSE) = FALSE;
