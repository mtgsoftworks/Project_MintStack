-- V16__portfolio_order_lifecycle_and_commission_model.sql
-- Adds advanced commission parameters and order lifecycle fields.

ALTER TABLE portfolios
    ADD COLUMN IF NOT EXISTS minimum_commission_amount DECIMAL(18, 6) NOT NULL DEFAULT 1.000000,
    ADD COLUMN IF NOT EXISTS commission_tax_rate DECIMAL(8, 6) NOT NULL DEFAULT 0.050000;

ALTER TABLE portfolio_transactions
    ADD COLUMN IF NOT EXISTS order_status VARCHAR(30) NOT NULL DEFAULT 'FILLED',
    ADD COLUMN IF NOT EXISTS filled_quantity DECIMAL(18, 6) NOT NULL DEFAULT 0.000000,
    ADD COLUMN IF NOT EXISTS average_fill_price DECIMAL(18, 6),
    ADD COLUMN IF NOT EXISTS filled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancel_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS realized_profit_loss DECIMAL(18, 6) NOT NULL DEFAULT 0.000000;

UPDATE portfolio_transactions
SET filled_quantity = quantity,
    average_fill_price = COALESCE(average_fill_price, price),
    order_status = 'FILLED'
WHERE order_status IS NULL
   OR order_status = ''
   OR transaction_type IN ('BUY', 'SELL');

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolios_minimum_commission_non_negative'
    ) THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT chk_portfolios_minimum_commission_non_negative
            CHECK (minimum_commission_amount >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolios_commission_tax_rate_range'
    ) THEN
        ALTER TABLE portfolios
            ADD CONSTRAINT chk_portfolios_commission_tax_rate_range
            CHECK (commission_tax_rate >= 0 AND commission_tax_rate <= 0.300000);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolio_transactions_order_status'
    ) THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_order_status
            CHECK (order_status IN ('PENDING', 'PARTIALLY_FILLED', 'FILLED', 'CANCELED', 'REJECTED'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_portfolio_transactions_filled_quantity_non_negative'
    ) THEN
        ALTER TABLE portfolio_transactions
            ADD CONSTRAINT chk_portfolio_transactions_filled_quantity_non_negative
            CHECK (filled_quantity >= 0);
    END IF;
END $$;
