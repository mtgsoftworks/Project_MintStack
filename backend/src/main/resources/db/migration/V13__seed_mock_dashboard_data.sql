-- Seed baseline dashboard data for empty environments
-- Idempotent inserts for news categories, news feed, and missing market instruments.

-- ---------------------------------------------------------------------------
-- News categories
-- ---------------------------------------------------------------------------
INSERT INTO news_categories (
    id, name, slug, description, display_order, is_active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    'Piyasa',
    'piyasa',
    'Genel piyasa gelismeleri',
    1,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news_categories WHERE slug = 'piyasa'
);

INSERT INTO news_categories (
    id, name, slug, description, display_order, is_active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    'Ekonomi',
    'ekonomi',
    'Makro ekonomi ve politika haberleri',
    2,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news_categories WHERE slug = 'ekonomi'
);

INSERT INTO news_categories (
    id, name, slug, description, display_order, is_active, created_at, updated_at, version
)
SELECT
    gen_random_uuid(),
    'Sirket',
    'sirket',
    'Sirket bilanco ve operasyon haberleri',
    3,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news_categories WHERE slug = 'sirket'
);

-- ---------------------------------------------------------------------------
-- Latest news (5 mock rows for dashboard widget)
-- ---------------------------------------------------------------------------
INSERT INTO news (
    id,
    category_id,
    title,
    summary,
    content,
    source_url,
    source_name,
    image_url,
    published_at,
    is_featured,
    is_published,
    view_count,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM news_categories WHERE slug = 'piyasa' LIMIT 1),
    'BIST 100 haftaya pozitif acilis yapti',
    'BIST 100 endeksi yeni haftaya alim agirlikli basladi.',
    'BIST 100 endeksi bankacilik ve holding hisselerinin destegiyle acilista yukselis kaydetti.',
    'https://mintstack.local/mock-news/bist-100-pozitif-acilis',
    'MintStack Mock',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '10 minutes',
    TRUE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news WHERE source_url = 'https://mintstack.local/mock-news/bist-100-pozitif-acilis'
);

INSERT INTO news (
    id,
    category_id,
    title,
    summary,
    content,
    source_url,
    source_name,
    image_url,
    published_at,
    is_featured,
    is_published,
    view_count,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM news_categories WHERE slug = 'ekonomi' LIMIT 1),
    'TCMB rezervlerinde toparlanma sinyali',
    'Rezerv verilerinde haftalik bazda sinirli artis goruldu.',
    'Son aciklanan veriler, rezervlerdeki dengelenmenin surdugune isaret ediyor.',
    'https://mintstack.local/mock-news/tcmb-rezerv-toparlanma',
    'MintStack Mock',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '35 minutes',
    FALSE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news WHERE source_url = 'https://mintstack.local/mock-news/tcmb-rezerv-toparlanma'
);

INSERT INTO news (
    id,
    category_id,
    title,
    summary,
    content,
    source_url,
    source_name,
    image_url,
    published_at,
    is_featured,
    is_published,
    view_count,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM news_categories WHERE slug = 'sirket' LIMIT 1),
    'Bankacilik hisselerinde gun ici hacim artisi',
    'Sektor hisselerinde gun ortasinda yuksek islem hacmi izlendi.',
    'Banka endeksinde artan hacimle birlikte volatilite de yukselis gostermeye devam etti.',
    'https://mintstack.local/mock-news/bankacilik-hacim-artisi',
    'MintStack Mock',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    FALSE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news WHERE source_url = 'https://mintstack.local/mock-news/bankacilik-hacim-artisi'
);

INSERT INTO news (
    id,
    category_id,
    title,
    summary,
    content,
    source_url,
    source_name,
    image_url,
    published_at,
    is_featured,
    is_published,
    view_count,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM news_categories WHERE slug = 'piyasa' LIMIT 1),
    'Tahvil faizlerinde yatay seyir korunuyor',
    'Gosterge tahvil faizleri gunun ilk yarisinda dengeli seyretti.',
    'Tahvil piyasasinda sinirli oynaklik gorulurken, getiri egrisi dengeli kaldi.',
    'https://mintstack.local/mock-news/tahvil-faiz-yatay',
    'MintStack Mock',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '1 hour 30 minutes',
    FALSE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news WHERE source_url = 'https://mintstack.local/mock-news/tahvil-faiz-yatay'
);

INSERT INTO news (
    id,
    category_id,
    title,
    summary,
    content,
    source_url,
    source_name,
    image_url,
    published_at,
    is_featured,
    is_published,
    view_count,
    created_at,
    updated_at,
    version
)
SELECT
    gen_random_uuid(),
    (SELECT id FROM news_categories WHERE slug = 'ekonomi' LIMIT 1),
    'Fon girislerinde teknoloji temasi one cikti',
    'Yatirim fonlarinda teknoloji temali urunlere talep suruyor.',
    'Haftalik dagilimda hisse yogun fonlarin payi artarken, karma fonlarda dengelenme izlendi.',
    'https://mintstack.local/mock-news/fon-teknoloji-talebi',
    'MintStack Mock',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    FALSE,
    TRUE,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM news WHERE source_url = 'https://mintstack.local/mock-news/fon-teknoloji-talebi'
);

-- ---------------------------------------------------------------------------
-- Missing market instruments for dashboard related pages
-- ---------------------------------------------------------------------------
-- Bonds
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TRT120128T11', '2Y Sabit Kupon Devlet Tahvili', 'BOND', 'BIST', 'TRY',
    98.450000, 98.100000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TRT120128T11');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TRT150130T16', '5Y Sabit Kupon Devlet Tahvili', 'BOND', 'BIST', 'TRY',
    95.300000, 95.120000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TRT150130T16');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TRT080328T15', '10Y Sabit Kupon Devlet Tahvili', 'BOND', 'BIST', 'TRY',
    90.850000, 90.620000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TRT080328T15');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TRT220932T11', 'Uzun Vade Devlet Tahvili', 'BOND', 'BIST', 'TRY',
    88.120000, 88.010000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TRT220932T11');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TRT180226T13', 'Orta Vade Devlet Tahvili', 'BOND', 'BIST', 'TRY',
    97.670000, 97.420000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TRT180226T13');

-- Funds
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'MAC', 'Hisse Senedi Yogun Fon', 'FUND', 'TEFAS', 'TRY',
    12.540000, 12.420000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'MAC');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TCD', 'Degisken Fon', 'FUND', 'TEFAS', 'TRY',
    8.930000, 8.880000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TCD');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'TI2', 'Para Piyasasi Fonu', 'FUND', 'TEFAS', 'TRY',
    4.270000, 4.260000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'TI2');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'AFT', 'Altin Katilim Fonu', 'FUND', 'TEFAS', 'TRY',
    22.180000, 21.940000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'AFT');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'GSP', 'Girisim Sermayesi Fonu', 'FUND', 'TEFAS', 'TRY',
    31.750000, 31.300000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'GSP');

-- VIOP
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'F_XU0300426', 'BIST30 Nisan Vadeli', 'VIOP', 'VIOP', 'TRY',
    11120.000000, 11065.000000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'F_XU0300426');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'F_USDTRY0426', 'USDTRY Nisan Vadeli', 'VIOP', 'VIOP', 'TRY',
    39.220000, 39.080000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'F_USDTRY0426');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'F_GARAN0426', 'GARAN Nisan Vadeli', 'VIOP', 'VIOP', 'TRY',
    129.400000, 127.950000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'F_GARAN0426');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'F_THYAO0426', 'THYAO Nisan Vadeli', 'VIOP', 'VIOP', 'TRY',
    291.800000, 289.400000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'F_THYAO0426');

INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'F_XAUUSD0426', 'Ons Altin Nisan Vadeli', 'VIOP', 'VIOP', 'TRY',
    2895.000000, 2878.000000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'F_XAUUSD0426');

-- Dashboard BIST 100 symbol used by frontend
INSERT INTO instruments (
    id, symbol, name, type, exchange, currency, current_price, previous_close, is_active, is_simulated, created_at, updated_at, version
)
SELECT
    gen_random_uuid(), 'XU100.IS', 'BIST 100', 'INDEX', 'BIST', 'TRY',
    9850.000000, 9805.000000, TRUE, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM instruments WHERE symbol = 'XU100.IS');
