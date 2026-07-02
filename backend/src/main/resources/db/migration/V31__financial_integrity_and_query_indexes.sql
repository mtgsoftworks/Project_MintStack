-- Preserve financial history and enforce invariants at the database boundary.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

ALTER TABLE user_api_configs
    ALTER COLUMN api_key TYPE TEXT,
    ALTER COLUMN secret_key TYPE TEXT;

ALTER TABLE portfolio_transactions
    DROP CONSTRAINT IF EXISTS portfolio_transactions_portfolio_id_fkey;

ALTER TABLE portfolio_transactions
    ADD CONSTRAINT fk_portfolio_transactions_portfolio
    FOREIGN KEY (portfolio_id) REFERENCES portfolios(id) ON DELETE RESTRICT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_portfolios_balances_non_negative') THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT chk_portfolios_balances_non_negative
            CHECK (initial_cash_balance >= 0 AND cash_balance >= 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_portfolio_items_values_positive') THEN
        ALTER TABLE portfolio_items
            ADD CONSTRAINT chk_portfolio_items_values_positive
            CHECK (quantity > 0 AND purchase_price > 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_portfolio_transactions_values_positive') THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_values_positive
            CHECK (quantity > 0 AND price > 0);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_portfolio_transactions_fill_consistency') THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_fill_consistency
            CHECK (
                filled_quantity >= 0
                AND filled_quantity <= quantity
                AND (average_fill_price IS NULL OR average_fill_price > 0)
            );
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_currency_rates_source_code_fetched
    ON currency_rates(source, currency_code, fetched_at DESC);

CREATE INDEX IF NOT EXISTS idx_news_published_status_date
    ON news(is_published, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_news_category_published_date
    ON news(category_id, is_published, published_at DESC);

CREATE INDEX IF NOT EXISTS idx_portfolio_transactions_status_created
    ON portfolio_transactions(portfolio_id, order_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_portfolio_transactions_history
    ON portfolio_transactions(portfolio_id, transaction_date DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_price_alerts_pending_instrument
    ON price_alerts(instrument_id)
    WHERE is_active = TRUE AND is_triggered = FALSE;

CREATE INDEX IF NOT EXISTS idx_instruments_symbol_trgm
    ON instruments USING GIN (LOWER(symbol) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_instruments_name_trgm
    ON instruments USING GIN (LOWER(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_news_title_trgm
    ON news USING GIN (LOWER(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_news_summary_trgm
    ON news USING GIN (LOWER(summary) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_users_email_trgm
    ON users USING GIN (LOWER(email) gin_trgm_ops);
