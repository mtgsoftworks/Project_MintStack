-- Seed current-day VIOP history rows with baseline volume for UI visibility.
-- Idempotent for each instrument and date due UNIQUE(instrument_id, price_date).

INSERT INTO price_history (
    id,
    instrument_id,
    open_price,
    high_price,
    low_price,
    close_price,
    adj_close,
    volume,
    price_date
)
SELECT
    gen_random_uuid(),
    i.id,
    i.current_price,
    i.current_price,
    i.current_price,
    i.current_price,
    i.current_price,
    CASE i.symbol
        WHEN 'F_XU0300426' THEN 145000
        WHEN 'F_USDTRY0426' THEN 89000
        WHEN 'F_GARAN0426' THEN 76000
        WHEN 'F_THYAO0426' THEN 82000
        WHEN 'F_XAUUSD0426' THEN 68000
        ELSE 50000
    END,
    CURRENT_DATE
FROM instruments i
WHERE i.type = 'VIOP'
  AND i.is_active = TRUE
  AND NOT EXISTS (
      SELECT 1
      FROM price_history ph
      WHERE ph.instrument_id = i.id
        AND ph.price_date = CURRENT_DATE
  );
