-- News pipeline hardening:
-- 1) Add explicit simulation marker
-- 2) Add external hash for dedup
-- 3) Remove mock seed rows and normalize legacy simulation rows
-- 4) Add uniqueness guarantees for real feed ingestion

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS is_simulated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE news
    ADD COLUMN IF NOT EXISTS external_hash VARCHAR(64);

UPDATE news
SET is_simulated = TRUE
WHERE COALESCE(source_name, '') ILIKE 'Simulasyon%'
   OR COALESCE(source_url, '') ILIKE '%mintstack.local/simulation-news%';

DELETE FROM news
WHERE source_name = 'MintStack Mock'
   OR COALESCE(source_url, '') ILIKE '%mintstack.local/mock-news/%';

UPDATE news
SET published_at = COALESCE(published_at, created_at, CURRENT_TIMESTAMP)
WHERE published_at IS NULL;

UPDATE news
SET external_hash = md5(
    lower(COALESCE(trim(title), '')) || '|' ||
    lower(COALESCE(trim(source_name), '')) || '|' ||
    lower(COALESCE(trim(source_url), '')) || '|' ||
    COALESCE(to_char(published_at, 'YYYY-MM-DD"T"HH24:MI:SS'), '')
)
WHERE external_hash IS NULL;

WITH ranked_source_url AS (
    SELECT ctid,
           row_number() OVER (
               PARTITION BY source_url
               ORDER BY published_at DESC NULLS LAST, created_at DESC NULLS LAST
           ) AS rn
    FROM news
    WHERE source_url IS NOT NULL
)
DELETE FROM news n
USING ranked_source_url r
WHERE n.ctid = r.ctid
  AND r.rn > 1;

WITH ranked_external_hash AS (
    SELECT ctid,
           row_number() OVER (
               PARTITION BY external_hash
               ORDER BY published_at DESC NULLS LAST, created_at DESC NULLS LAST
           ) AS rn
    FROM news
    WHERE external_hash IS NOT NULL
)
DELETE FROM news n
USING ranked_external_hash r
WHERE n.ctid = r.ctid
  AND r.rn > 1;

CREATE INDEX IF NOT EXISTS idx_news_simulated ON news(is_simulated);
CREATE UNIQUE INDEX IF NOT EXISTS ux_news_source_url_not_null ON news(source_url) WHERE source_url IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_news_external_hash_not_null ON news(external_hash) WHERE external_hash IS NOT NULL;

