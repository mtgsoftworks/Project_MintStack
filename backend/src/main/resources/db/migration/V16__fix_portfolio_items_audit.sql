-- Fix P0: Add missing audit columns and unique constraint to portfolio_items
-- Required by BaseEntity (createdAt, updatedAt, version) and JPA entity unique constraint

-- Add audit columns
ALTER TABLE portfolio_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE portfolio_items ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
ALTER TABLE portfolio_items ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add missing unique constraint (JPA expects this)
-- Note: uniqueConstraints expects purchase_date in the constraint per PortfolioItem.java
ALTER TABLE portfolio_items ADD CONSTRAINT uk_portfolio_instrument_date
    UNIQUE (portfolio_id, instrument_id, purchase_date);

-- Update sequence to handle existing data
ALTER SEQUENCE portfolio_items_id_seq RESTART WITH 1;

-- Backfill created_at for existing rows if needed
UPDATE portfolio_items SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
