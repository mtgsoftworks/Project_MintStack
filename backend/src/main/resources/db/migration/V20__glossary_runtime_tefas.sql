-- Glossary, runtime settings and provider enum expansion.

CREATE TABLE IF NOT EXISTS glossary_terms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    term VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    category VARCHAR(80) NOT NULL,
    definition TEXT NOT NULL,
    aliases VARCHAR(1000),
    locale VARCHAR(8) NOT NULL DEFAULT 'tr',
    source_name VARCHAR(120),
    source_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_glossary_terms_slug_locale UNIQUE (slug, locale)
);

CREATE INDEX IF NOT EXISTS idx_glossary_terms_category ON glossary_terms(category);
CREATE INDEX IF NOT EXISTS idx_glossary_terms_active ON glossary_terms(is_active);
CREATE INDEX IF NOT EXISTS idx_glossary_terms_term ON glossary_terms(term);

CREATE TABLE IF NOT EXISTS runtime_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(160) NOT NULL UNIQUE,
    setting_value TEXT NOT NULL,
    description TEXT,
    restart_required BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by VARCHAR(160),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

ALTER TABLE user_api_configs DROP CONSTRAINT IF EXISTS chk_user_api_provider;
ALTER TABLE user_api_configs
    ADD CONSTRAINT chk_user_api_provider
    CHECK (provider IN (
        'YAHOO_FINANCE',
        'ALPHA_VANTAGE',
        'FINNHUB',
        'TCMB',
        'TEFAS',
        'FINTABLES',
        'RSS',
        'LLM_ENRICHMENT',
        'OTHER'
    ));

INSERT INTO runtime_settings (setting_key, setting_value, description, restart_required, updated_by)
VALUES
    ('app.rate-limit.store', 'redis', 'Rate limit store: redis or memory.', false, 'migration'),
    ('app.fintables.enabled', 'false', 'Fintables adapter is passive by default; enable only after API key/base URL are configured.', false, 'migration'),
    ('app.news.llm.enabled', 'false', 'Optional LLM news enrichment flag. RSS ingestion stays primary.', false, 'migration'),
    ('app.external-api.tefas.enabled', 'true', 'TEFAS public fund data integration enabled.', false, 'migration'),
    ('app.scheduler.fund-prices-cron', '0 30 18 * * MON-FRI', 'Fund/NAV update cadence; end-of-day by default.', true, 'migration'),
    ('app.scheduler.forex-rates-cron', '0 30 10,16 * * MON-FRI', 'No 24/7 FX requirement; TCMB/non-live refresh windows.', true, 'migration')
ON CONFLICT (setting_key) DO UPDATE SET
    setting_value = EXCLUDED.setting_value,
    description = EXCLUDED.description,
    restart_required = EXCLUDED.restart_required,
    updated_by = EXCLUDED.updated_by,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO glossary_terms (
    term, slug, category, definition, aliases, locale, source_name, source_url, sort_order
) VALUES
    (
        'TEFAS',
        'tefas',
        'Fon',
        'Turkiye Elektronik Fon Alim Satim Platformu; yatirim fonlari icin fon bilgisi, getiri, fiyat ve karsilastirma verilerinin izlenebildigi merkezi platformdur.',
        'Turkiye Elektronik Fon Alim Satim Platformu,Fon Bilgilendirme Platformu',
        'tr',
        'TEFAS',
        'https://www.tefas.gov.tr/',
        10
    ),
    (
        'Fon Kodu',
        'fon-kodu',
        'Fon',
        'TEFAS ve portfoy yonetim sirketleri tarafindan fonu ayirt etmek icin kullanilan kisa koddur.',
        'fon sembolu,fon kodu',
        'tr',
        'TEFAS',
        'https://tefasweb.ingetechnology.com/tr/fon-getirileri',
        20
    ),
    (
        'Fon Fiyati',
        'fon-fiyati',
        'Fon',
        'Bir fon katilma payinin ilgili tarih icin hesaplanan birim fiyatidir. Portfoy degeri, pay sayisi ve operasyonel zamanlama nedeniyle gun sonunda guncellenebilir.',
        'birim pay fiyati,NAV,katilma payi fiyati',
        'tr',
        'TEFAS',
        'https://tefasweb.ingetechnology.com/tr/fon-getirileri',
        30
    ),
    (
        'Portfoy Buyuklugu',
        'portfoy-buyuklugu',
        'Fon',
        'Fonun yonettigi toplam varlik degerini ifade eder. Fonun olcegini, likidite profilini ve piyasadaki agirligini yorumlamak icin kullanilir.',
        'fon buyuklugu,toplam portfoy degeri',
        'tr',
        'TEFAS',
        'https://www.tefas.gov.tr/',
        40
    ),
    (
        'Borsa Yatirim Fonu',
        'borsa-yatirim-fonu',
        'Fon',
        'Borsada pay gibi islem goren, genellikle bir endeks veya varlik sepetini takip eden fon turudur.',
        'BYF,ETF',
        'tr',
        'TEFAS',
        'https://tefasweb.ingetechnology.com/tr/fon-getirileri',
        50
    ),
    (
        'Hisse Senedi',
        'hisse-senedi',
        'Borsa',
        'Bir anonim ortakliktaki ortaklik payini temsil eden sermaye piyasasi aracidir.',
        'pay,pay senedi',
        'tr',
        'Borsa Istanbul',
        'https://www.borsaistanbul.com/',
        100
    ),
    (
        'Endeks',
        'endeks',
        'Borsa',
        'Belirli kurallara gore secilen menkul kiymetlerin fiyat performansini olcen gostergedir.',
        'borsa endeksi,BIST 100',
        'tr',
        'Borsa Istanbul',
        'https://www.borsaistanbul.com/',
        110
    ),
    (
        'Limit Emir',
        'limit-emir',
        'Islem',
        'Alim veya satim icin kullanicinin belirledigi fiyat seviyesine bagli emir tipidir.',
        'limit order',
        'tr',
        'MintStack',
        'https://mintstack.local',
        200
    ),
    (
        'Piyasa Emri',
        'piyasa-emri',
        'Islem',
        'Emrin mevcut piyasa kosullarindaki uygun fiyatla gerceklesmesini hedefleyen emir tipidir.',
        'market order',
        'tr',
        'MintStack',
        'https://mintstack.local',
        210
    ),
    (
        'Doviz Kuru',
        'doviz-kuru',
        'Doviz',
        'Bir para biriminin baska bir para birimi karsisindaki alis veya satis degeridir. Portalda TCMB ve secili dis kaynaklardan periyodik olarak guncellenir.',
        'kur,fx,forex',
        'tr',
        'TCMB',
        'https://www.tcmb.gov.tr/kurlar',
        300
    )
ON CONFLICT (slug, locale) DO UPDATE SET
    term = EXCLUDED.term,
    category = EXCLUDED.category,
    definition = EXCLUDED.definition,
    aliases = EXCLUDED.aliases,
    source_name = EXCLUDED.source_name,
    source_url = EXCLUDED.source_url,
    is_active = TRUE,
    sort_order = EXCLUDED.sort_order,
    updated_at = CURRENT_TIMESTAMP;
