-- Fix P0: Add missing audit columns and unique constraint to portfolio_items
-- Required by BaseEntity (createdAt, updatedAt, version) and JPA entity unique constraint

-- Add audit columns
ALTER TABLE portfolio_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE portfolio_items ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE portfolio_items ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add missing unique constraint (JPA expects this)
-- Note: uniqueConstraints expects purchase_date in the constraint per PortfolioItem.java
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_portfolio_instrument'
          AND conrelid = 'portfolio_items'::regclass
    ) THEN
        ALTER TABLE portfolio_items
            ADD CONSTRAINT uk_portfolio_instrument
            UNIQUE (portfolio_id, instrument_id, purchase_date);
    END IF;
END
$$;

-- Backfill created_at for existing rows if needed
UPDATE portfolio_items SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
