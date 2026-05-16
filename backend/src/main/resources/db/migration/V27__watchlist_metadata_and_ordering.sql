-- Add watchlist metadata and stable item ordering support.

ALTER TABLE IF EXISTS watchlists
    ADD COLUMN IF NOT EXISTS tag VARCHAR(50);

ALTER TABLE IF EXISTS watchlists
    ADD COLUMN IF NOT EXISTS notes TEXT;

ALTER TABLE IF EXISTS watchlists
    ADD COLUMN IF NOT EXISTS column_prefs VARCHAR(300);

ALTER TABLE IF EXISTS watchlist_items
    ADD COLUMN IF NOT EXISTS display_order INTEGER;

WITH ordered_items AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY watchlist_id
            ORDER BY added_at NULLS LAST, created_at NULLS LAST, id
        ) AS row_no
    FROM watchlist_items
)
UPDATE watchlist_items wi
SET display_order = ordered_items.row_no
FROM ordered_items
WHERE wi.id = ordered_items.id
  AND (wi.display_order IS NULL OR wi.display_order <= 0);

UPDATE watchlist_items
SET display_order = 1
WHERE display_order IS NULL OR display_order <= 0;

ALTER TABLE IF EXISTS watchlist_items
    ALTER COLUMN display_order SET NOT NULL;

ALTER TABLE IF EXISTS watchlist_items
    ALTER COLUMN display_order SET DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_watchlist_items_watchlist_order
    ON watchlist_items(watchlist_id, display_order);
