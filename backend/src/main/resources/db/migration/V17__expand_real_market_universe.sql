-- Expand baseline market universe with real/popular TR instruments.
-- Sources used for symbol sets:
-- - OYAK securities pages (TR government bond and fund symbol lists)
-- - Borsa Istanbul VIOP contract group listings (pay vadeli underlying symbols)

-- ---------------------------------------------------------------------------
-- Bonds (TRT government securities)
-- ---------------------------------------------------------------------------
WITH bond_seed(symbol, name, current_price, previous_close) AS (
    VALUES
        ('TRT021030T18', 'Devlet Tahvili TRT021030T18', 97.971000, 97.820000),
        ('TRT120929T12', 'Devlet Tahvili TRT120929T12', 97.679000, 97.540000),
        ('TRT051033T12', 'Devlet Tahvili TRT051033T12', 97.190000, 97.030000),
        ('TRT081128T15', 'Devlet Tahvili TRT081128T15', 96.610000, 96.450000),
        ('TRT131027T36', 'Devlet Tahvili TRT131027T36', 96.328000, 96.180000),
        ('TRT061228T16', 'Devlet Tahvili TRT061228T16', 95.800000, 95.640000),
        ('TRT160627T13', 'Devlet Tahvili TRT160627T13', 95.584000, 95.430000),
        ('TRT070329T15', 'Devlet Tahvili TRT070329T15', 95.560000, 95.420000),
        ('TRT100730T13', 'Devlet Tahvili TRT100730T13', 95.454000, 95.300000),
        ('TRT100227T13', 'Devlet Tahvili TRT100227T13', 95.380000, 95.220000),
        ('TRT090130T12', 'Devlet Tahvili TRT090130T12', 95.345000, 95.190000),
        ('TRT050935T13', 'Devlet Tahvili TRT050935T13', 95.220000, 95.080000),
        ('TRT270934T18', 'Devlet Tahvili TRT270934T18', 95.200000, 95.050000),
        ('TRT140727T14', 'Devlet Tahvili TRT140727T14', 95.178000, 95.020000),
        ('TRT060928T11', 'Devlet Tahvili TRT060928T11', 95.052000, 94.900000),
        ('TRT120826T16', 'Devlet Tahvili TRT120826T16', 95.039000, 94.890000),
        ('TRT020529T18', 'Devlet Tahvili TRT020529T18', 97.847000, 97.700000),
        ('TRT160431T19', 'Devlet Tahvili TRT160431T19', 95.065000, 94.920000),
        ('TRT060127T10', 'Devlet Tahvili TRT060127T10', 96.795000, 96.650000),
        ('TRT050630T11', 'Devlet Tahvili TRT050630T11', 95.335000, 95.190000),
        ('TRT050527T33', 'Devlet Tahvili TRT050527T33', 95.365000, 95.210000),
        ('TRT040729T14', 'Devlet Tahvili TRT040729T14', 95.552000, 95.400000),
        ('TRT130629T30', 'Devlet Tahvili TRT130629T30', 95.551000, 95.390000)
)
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    seed.symbol,
    seed.name,
    'BOND',
    'BIST',
    'TRY',
    seed.current_price,
    seed.previous_close,
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM bond_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM instruments i WHERE i.symbol = seed.symbol
);

-- ---------------------------------------------------------------------------
-- Funds (popular TEFAS symbols)
-- ---------------------------------------------------------------------------
WITH fund_seed(symbol, name, current_price, previous_close) AS (
    VALUES
        ('MAC', 'Hisse Senedi Fonu', 16.150000, 16.010000),
        ('TCD', 'Degisken Fon', 8.918000, 8.904000),
        ('TI2', 'Para Piyasasi Fonu', 4.274000, 4.266000),
        ('AFT', 'Yabanci Hisse Senedi Fonu', 42.478400, 42.060000),
        ('GSP', 'Girisim Sermayesi Yatirim Fonu', 113.570000, 113.100000),
        ('MTV', 'Hisse Senedi Fonu', 11.252000, 11.190000),
        ('IDH', 'BIST 100 Disi Sirketler Hisse Senedi Fonu', 8.180000, 8.130000),
        ('DVT', 'Doviz Fonu', 4.841000, 4.829000),
        ('GMR', 'Birinci Hisse Senedi Fonu', 8.454000, 8.418000),
        ('OPI', 'Birinci Hisse Senedi Fonu', 130.429000, 129.900000),
        ('YAS', 'Yabanci Teknoloji Sektor Hisse Senedi Fonu', 4.092000, 4.071000),
        ('NNF', 'Hisse Senedi Fonu', 129.037000, 128.500000),
        ('GL1', 'Altin Fonu', 7.432000, 7.406000),
        ('YNK', 'Hisse Senedi Fonu', 10.949000, 10.918000),
        ('DTL', 'Degisken Fon', 5.882000, 5.866000),
        ('YLB', 'Ikinci Serbest Fon', 5.891000, 5.878000),
        ('YCL', 'Kisa Vadeli Borclanma Araclari Fonu', 6.371000, 6.364000),
        ('YHS', 'Hisse Senedi Fonu', 6.875000, 6.848000),
        ('KVT', 'Kira Sertifikasi Katilim Fonu', 6.352000, 6.345000),
        ('FFS', 'Fon Sepeti Fonu', 6.265000, 6.254000)
)
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    seed.symbol,
    seed.name,
    'FUND',
    'TEFAS',
    'TRY',
    seed.current_price,
    seed.previous_close,
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM fund_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM instruments i WHERE i.symbol = seed.symbol
);

-- ---------------------------------------------------------------------------
-- VIOP contracts (popular underlying assets)
-- ---------------------------------------------------------------------------
WITH viop_seed(symbol, name, current_price, previous_close) AS (
    VALUES
        ('F_AKBNK0426', 'AKBNK Nisan Vadeli', 67.250000, 66.900000),
        ('F_ASELS0426', 'ASELS Nisan Vadeli', 74.800000, 74.100000),
        ('F_BIMAS0426', 'BIMAS Nisan Vadeli', 524.000000, 519.500000),
        ('F_EKGYO0426', 'EKGYO Nisan Vadeli', 14.850000, 14.700000),
        ('F_EREGL0426', 'EREGL Nisan Vadeli', 51.200000, 50.800000),
        ('F_FROTO0426', 'FROTO Nisan Vadeli', 995.000000, 986.500000),
        ('F_GUBRF0426', 'GUBRF Nisan Vadeli', 191.400000, 189.200000),
        ('F_ISCTR0426', 'ISCTR Nisan Vadeli', 16.180000, 16.030000),
        ('F_KCHOL0426', 'KCHOL Nisan Vadeli', 224.700000, 222.100000),
        ('F_KOZAL0426', 'KOZAL Nisan Vadeli', 34.950000, 34.500000),
        ('F_KOZAA0426', 'KOZAA Nisan Vadeli', 73.600000, 72.900000),
        ('F_PETKM0426', 'PETKM Nisan Vadeli', 24.760000, 24.510000),
        ('F_PGSUS0426', 'PGSUS Nisan Vadeli', 258.300000, 255.000000),
        ('F_SAHOL0426', 'SAHOL Nisan Vadeli', 102.900000, 101.800000),
        ('F_SISE0426', 'SISE Nisan Vadeli', 46.980000, 46.550000),
        ('F_TCELL0426', 'TCELL Nisan Vadeli', 103.250000, 102.100000),
        ('F_TUPRS0426', 'TUPRS Nisan Vadeli', 169.800000, 168.000000),
        ('F_VAKBN0426', 'VAKBN Nisan Vadeli', 31.420000, 31.100000),
        ('F_YKBNK0426', 'YKBNK Nisan Vadeli', 31.750000, 31.450000),
        ('F_EURTRY0426', 'EURTRY Nisan Vadeli', 42.580000, 42.340000)
)
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    seed.symbol,
    seed.name,
    'VIOP',
    'VIOP',
    'TRY',
    seed.current_price,
    seed.previous_close,
    TRUE,
    FALSE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM viop_seed seed
WHERE NOT EXISTS (
    SELECT 1 FROM instruments i WHERE i.symbol = seed.symbol
);

-- Seed current-day volume rows for newly available VIOP contracts.
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
        WHEN 'F_AKBNK0426' THEN 94000
        WHEN 'F_ASELS0426' THEN 88000
        WHEN 'F_BIMAS0426' THEN 61000
        WHEN 'F_EKGYO0426' THEN 72000
        WHEN 'F_EREGL0426' THEN 83000
        WHEN 'F_FROTO0426' THEN 47000
        WHEN 'F_GUBRF0426' THEN 51000
        WHEN 'F_ISCTR0426' THEN 97000
        WHEN 'F_KCHOL0426' THEN 64000
        WHEN 'F_KOZAL0426' THEN 53000
        WHEN 'F_KOZAA0426' THEN 56000
        WHEN 'F_PETKM0426' THEN 78000
        WHEN 'F_PGSUS0426' THEN 65000
        WHEN 'F_SAHOL0426' THEN 69000
        WHEN 'F_SISE0426' THEN 87000
        WHEN 'F_TCELL0426' THEN 74000
        WHEN 'F_TUPRS0426' THEN 62000
        WHEN 'F_VAKBN0426' THEN 80000
        WHEN 'F_YKBNK0426' THEN 84000
        WHEN 'F_EURTRY0426' THEN 57000
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
