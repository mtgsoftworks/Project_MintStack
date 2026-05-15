-- Keep crypto instruments usable for analysis and portfolio quick trades
-- when an external provider has not populated public crypto prices yet.
WITH crypto_seed(symbol, name, current_price, previous_close) AS (
    VALUES
        ('BTC-USD', 'Bitcoin', 95000.000000::numeric, 93575.000000::numeric),
        ('ETH-USD', 'Ethereum', 3400.000000::numeric, 3349.000000::numeric),
        ('BNB-USD', 'Binance Coin', 650.000000::numeric, 640.250000::numeric),
        ('SOL-USD', 'Solana', 180.000000::numeric, 177.300000::numeric),
        ('XRP-USD', 'XRP', 2.500000::numeric, 2.462500::numeric)
),
updated AS (
    UPDATE instruments instrument
    SET name = seed.name,
        exchange = 'CRYPTO',
        currency = 'USD',
        current_price = seed.current_price,
        previous_close = seed.previous_close,
        is_active = true,
        updated_at = CURRENT_TIMESTAMP
    FROM crypto_seed seed
    WHERE instrument.symbol = seed.symbol
      AND instrument.is_simulated = false
      AND instrument.type = 'CRYPTO'
      AND (
          instrument.current_price IS NULL
          OR instrument.current_price <= 0
          OR instrument.previous_close IS NULL
          OR instrument.previous_close <= 0
      )
    RETURNING instrument.symbol
)
INSERT INTO instruments (
    symbol,
    name,
    type,
    exchange,
    currency,
    current_price,
    previous_close,
    is_active,
    is_simulated,
    created_at,
    updated_at,
    version
)
SELECT
    seed.symbol,
    seed.name,
    'CRYPTO',
    'CRYPTO',
    'USD',
    seed.current_price,
    seed.previous_close,
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM crypto_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM instruments instrument
    WHERE instrument.symbol = seed.symbol
      AND instrument.is_simulated = false
);
