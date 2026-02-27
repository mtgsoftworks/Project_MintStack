-- V15__portfolio_cash_and_order_types.sql
-- Adds cash/commission model on portfolio and advanced order metadata on portfolio transactions.

ALTER TABLE portfolios
    ADD COLUMN IF NOT EXISTS initial_cash_balance DECIMAL(18, 6) NOT NULL DEFAULT 100000.000000,
    ADD COLUMN IF NOT EXISTS cash_balance DECIMAL(18, 6) NOT NULL DEFAULT 100000.000000,
    ADD COLUMN IF NOT EXISTS commission_rate DECIMAL(8, 6) NOT NULL DEFAULT 0.001000;

UPDATE portfolios
SET cash_balance = COALESCE(cash_balance, initial_cash_balance, 100000.000000),
    initial_cash_balance = COALESCE(initial_cash_balance, 100000.000000),
    commission_rate = COALESCE(commission_rate, 0.001000)
WHERE cash_balance IS NULL
   OR initial_cash_balance IS NULL
   OR commission_rate IS NULL;

ALTER TABLE portfolio_transactions
    ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) NOT NULL DEFAULT 'MARKET',
    ADD COLUMN IF NOT EXISTS commission_amount DECIMAL(18, 6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS limit_price DECIMAL(18, 6),
    ADD COLUMN IF NOT EXISTS stop_price DECIMAL(18, 6);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolio_commission_rate_range'
    ) THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT chk_portfolio_commission_rate_range
            CHECK (commission_rate >= 0 AND commission_rate <= 0.100000);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolio_transactions_order_type'
    ) THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_order_type
            CHECK (order_type IN ('MARKET', 'LIMIT', 'STOP'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolio_transactions_commission_non_negative'
    ) THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_commission_non_negative
            CHECK (commission_amount >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolio_transactions_limit_stop_non_negative'
    ) THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_limit_stop_non_negative
            CHECK (
                (limit_price IS NULL OR limit_price > 0)
                AND (stop_price IS NULL OR stop_price > 0)
            );
    END IF;
END $$;
