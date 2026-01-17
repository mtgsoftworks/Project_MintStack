-- V5__portfolio_transactions.sql
-- Portfolio transaction history table

CREATE TABLE IF NOT EXISTS portfolio_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id),
    transaction_type VARCHAR(20) NOT NULL,
    quantity DECIMAL(18, 6) NOT NULL,
    price DECIMAL(18, 6) NOT NULL,
    transaction_date DATE NOT NULL,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_portfolio_transaction_type CHECK (transaction_type IN ('BUY', 'SELL'))
);

CREATE INDEX idx_portfolio_transactions_portfolio ON portfolio_transactions(portfolio_id);
CREATE INDEX idx_portfolio_transactions_instrument ON portfolio_transactions(instrument_id);
CREATE INDEX idx_portfolio_transactions_date ON portfolio_transactions(transaction_date DESC);
