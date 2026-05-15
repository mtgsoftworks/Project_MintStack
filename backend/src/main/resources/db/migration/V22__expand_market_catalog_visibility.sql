-- Keep jury/demo catalog pages populated after admin market-data resets.
-- Live schedulers can still update these prices later; these rows provide a visible baseline.

WITH stock_seed(symbol, name, current_price, previous_close) AS (
    VALUES
        ('THYAO', 'Turk Hava Yollari', 296.40, 294.10),
        ('GARAN', 'Garanti BBVA', 114.80, 113.90),
        ('AKBNK', 'Akbank', 67.25, 66.90),
        ('ISCTR', 'Turkiye Is Bankasi C', 16.18, 16.03),
        ('YKBNK', 'Yapi Kredi Bankasi', 31.75, 31.45),
        ('VAKBN', 'VakifBank', 31.42, 31.10),
        ('HALKB', 'Halkbank', 20.34, 20.18),
        ('EREGL', 'Eregli Demir Celik', 51.20, 50.80),
        ('KRDMD', 'Kardemir D', 30.64, 30.18),
        ('SISE', 'Sisecam', 46.98, 46.55),
        ('PETKM', 'Petkim', 24.76, 24.51),
        ('TUPRS', 'Tupras', 169.80, 168.00),
        ('ASELS', 'Aselsan', 74.80, 74.10),
        ('BIMAS', 'BIM Birlesik Magazalar', 524.00, 519.50),
        ('MGROS', 'Migros Ticaret', 563.50, 558.00),
        ('ULKER', 'Ulker Biskuvi', 171.20, 169.80),
        ('AEFES', 'Anadolu Efes', 204.40, 202.80),
        ('CCOLA', 'Coca-Cola Icecek', 63.45, 62.90),
        ('KCHOL', 'Koc Holding', 224.70, 222.10),
        ('SAHOL', 'Sabanci Holding', 102.90, 101.80),
        ('ENKAI', 'Enka Insaat', 53.75, 53.20),
        ('DOHOL', 'Dogan Holding', 16.74, 16.58),
        ('ALARK', 'Alarko Holding', 134.20, 132.70),
        ('AGHOL', 'Anadolu Grubu Holding', 372.50, 368.00),
        ('BRYAT', 'Borusan Yatirim', 2310.00, 2285.00),
        ('FROTO', 'Ford Otosan', 995.00, 986.50),
        ('TOASO', 'Tofas', 266.10, 263.40),
        ('DOAS', 'Dogus Otomotiv', 211.80, 209.60),
        ('TTRAK', 'Turk Traktor', 735.00, 727.50),
        ('ARCLK', 'Arcelik', 155.60, 153.90),
        ('VESTL', 'Vestel', 78.35, 77.60),
        ('TCELL', 'Turkcell', 103.25, 102.10),
        ('PGSUS', 'Pegasus', 258.30, 255.00),
        ('TAVHL', 'TAV Havalimanlari', 100.79, 100.35),
        ('EKGYO', 'Emlak Konut GMYO', 14.85, 14.70),
        ('ISGYO', 'Is GMYO', 20.92, 20.70),
        ('KOZAL', 'Koza Altin', 34.95, 34.50),
        ('KOZAA', 'Koza Anadolu Metal', 73.60, 72.90),
        ('GUBRF', 'Gubre Fabrikalari', 191.40, 189.20),
        ('SASA', 'Sasa Polyester', 4.92, 4.86),
        ('HEKTS', 'Hektas', 4.74, 4.69),
        ('ASTOR', 'Astor Enerji', 112.60, 111.20),
        ('KONTR', 'Kontrolmatik', 49.82, 49.10),
        ('SMRTG', 'Smart Gunes Enerjisi', 47.36, 46.80),
        ('ENJSA', 'Enerjisa Enerji', 65.40, 64.85),
        ('AKSA', 'Aksa Akrilik', 13.28, 13.14),
        ('OYAKC', 'Oyak Cimento', 74.25, 73.60),
        ('MAVI', 'Mavi Giyim', 95.10, 94.15),
        ('ZOREN', 'Zorlu Enerji', 5.82, 5.76),
        ('CANTE', 'Can2 Termik', 17.36, 17.18)
),
updated AS (
    UPDATE instruments i
    SET
        name = seed.name,
        type = 'STOCK',
        exchange = 'BIST',
        currency = 'TRY',
        current_price = COALESCE(i.current_price, seed.current_price),
        previous_close = COALESCE(i.previous_close, seed.previous_close),
        is_active = TRUE,
        is_simulated = FALSE,
        updated_at = CURRENT_TIMESTAMP
    FROM stock_seed seed
    WHERE i.symbol = seed.symbol
      AND (i.is_simulated IS NULL OR i.is_simulated = FALSE)
    RETURNING i.symbol
)
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close,
    is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), seed.symbol, seed.name, 'STOCK', 'BIST', 'TRY',
    seed.current_price, seed.previous_close, TRUE, FALSE,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM stock_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM instruments i
    WHERE i.symbol = seed.symbol
      AND (i.is_simulated IS NULL OR i.is_simulated = FALSE)
);

UPDATE instruments
SET is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP
WHERE type IN ('BOND', 'VIOP', 'CRYPTO', 'INDEX')
  AND (is_simulated IS NULL OR is_simulated = FALSE)
  AND current_price IS NOT NULL;
