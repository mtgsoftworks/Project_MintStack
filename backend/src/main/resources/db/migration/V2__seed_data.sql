-- V2__seed_data.sql
-- MintStack Finance Portal - Seed Data

-- Insert News Categories
INSERT INTO news_categories (id, name, slug, description, display_order, is_active) VALUES
(gen_random_uuid(), 'Genel Ekonomi', 'genel-ekonomi', 'Genel ekonomi haberleri', 1, true),
(gen_random_uuid(), 'Hisse Senedi', 'hisse-senedi', 'Borsa ve hisse senedi haberleri', 2, true),
(gen_random_uuid(), 'Döviz', 'doviz', 'Döviz piyasası haberleri', 3, true),
(gen_random_uuid(), 'Tahvil/Bono', 'tahvil-bono', 'Tahvil ve bono piyasası haberleri', 4, true),
(gen_random_uuid(), 'Altın', 'altin', 'Altın ve değerli metaller', 5, true),
(gen_random_uuid(), 'Kripto', 'kripto', 'Kripto para haberleri', 6, true),
(gen_random_uuid(), 'Dünya Ekonomisi', 'dunya-ekonomisi', 'Uluslararası ekonomi haberleri', 7, true);

-- Insert Currency Instruments
INSERT INTO instruments (id, symbol, name, type, currency, is_active) VALUES
(gen_random_uuid(), 'USD/TRY', 'ABD Doları', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'EUR/TRY', 'Euro', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'GBP/TRY', 'İngiliz Sterlini', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'CHF/TRY', 'İsviçre Frangı', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'JPY/TRY', 'Japon Yeni', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'AUD/TRY', 'Avustralya Doları', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'CAD/TRY', 'Kanada Doları', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'SAR/TRY', 'Suudi Riyali', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'CNY/TRY', 'Çin Yuanı', 'CURRENCY', 'TRY', true),
(gen_random_uuid(), 'RUB/TRY', 'Rus Rublesi', 'CURRENCY', 'TRY', true);

-- Insert BIST Stock Instruments
INSERT INTO instruments (id, symbol, name, type, exchange, currency, is_active) VALUES
(gen_random_uuid(), 'THYAO', 'Türk Hava Yolları', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'GARAN', 'Garanti Bankası', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'AKBNK', 'Akbank', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'EREGL', 'Ereğli Demir Çelik', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'SISE', 'Şişecam', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'KCHOL', 'Koç Holding', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'SAHOL', 'Sabancı Holding', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'TUPRS', 'Tüpraş', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'ASELS', 'Aselsan', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'BIMAS', 'BİM Mağazalar', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'TCELL', 'Turkcell', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'PGSUS', 'Pegasus', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'SASA', 'Sasa Polyester', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'TOASO', 'Tofaş', 'STOCK', 'BIST', 'TRY', true),
(gen_random_uuid(), 'FROTO', 'Ford Otosan', 'STOCK', 'BIST', 'TRY', true);

-- Insert Bond Instruments
INSERT INTO instruments (id, symbol, name, type, currency, is_active) VALUES
(gen_random_uuid(), 'TBOND-2Y', 'Hazine Bonosu 2 Yıl', 'BOND', 'TRY', true),
(gen_random_uuid(), 'TBOND-5Y', 'Devlet Tahvili 5 Yıl', 'BOND', 'TRY', true),
(gen_random_uuid(), 'TBOND-10Y', 'Devlet Tahvili 10 Yıl', 'BOND', 'TRY', true),
(gen_random_uuid(), 'EUROBOND-30', 'Eurobond 2030', 'BOND', 'USD', true);

-- Insert Fund Instruments
INSERT INTO instruments (id, symbol, name, type, currency, is_active) VALUES
(gen_random_uuid(), 'AFT', 'Ak Portföy Teknoloji', 'FUND', 'TRY', true),
(gen_random_uuid(), 'IPB', 'İş Portföy BIST 30', 'FUND', 'TRY', true),
(gen_random_uuid(), 'YAF', 'Yapı Kredi Portföy Altın', 'FUND', 'TRY', true),
(gen_random_uuid(), 'TI2', 'TEB Portföy İkinci Hisse', 'FUND', 'TRY', true),
(gen_random_uuid(), 'GAF', 'Garanti Portföy Altın', 'FUND', 'TRY', true);

-- Insert VIOP Instruments
INSERT INTO instruments (id, symbol, name, type, exchange, currency, is_active) VALUES
(gen_random_uuid(), 'F_XU030', 'BIST 30 Vadeli', 'VIOP', 'VIOP', 'TRY', true),
(gen_random_uuid(), 'F_USDTRY', 'Dolar/TL Vadeli', 'VIOP', 'VIOP', 'TRY', true),
(gen_random_uuid(), 'F_EURTRY', 'Euro/TL Vadeli', 'VIOP', 'VIOP', 'TRY', true),
(gen_random_uuid(), 'F_GOLDTRY', 'Altın/TL Vadeli', 'VIOP', 'VIOP', 'TRY', true);

-- Insert Commodity Instruments
INSERT INTO instruments (id, symbol, name, type, currency, is_active) VALUES
(gen_random_uuid(), 'XAU/TRY', 'Ons Altın', 'COMMODITY', 'TRY', true),
(gen_random_uuid(), 'XAG/TRY', 'Ons Gümüş', 'COMMODITY', 'TRY', true),
(gen_random_uuid(), 'GRAM-ALTIN', 'Gram Altın', 'COMMODITY', 'TRY', true),
(gen_random_uuid(), 'CEYREK-ALTIN', 'Çeyrek Altın', 'COMMODITY', 'TRY', true);
