-- V1__initial_schema.sql
-- MintStack Finance Portal - Initial Database Schema

-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_users_keycloak_id ON users(keycloak_id);
CREATE INDEX idx_users_email ON users(email);

-- News Categories Table
CREATE TABLE news_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- News Table
CREATE TABLE news (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID REFERENCES news_categories(id),
    title VARCHAR(500) NOT NULL,
    summary VARCHAR(1000),
    content TEXT,
    source_url VARCHAR(500),
    source_name VARCHAR(200),
    image_url VARCHAR(500),
    published_at TIMESTAMP,
    is_featured BOOLEAN DEFAULT FALSE,
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_news_category ON news(category_id);
CREATE INDEX idx_news_published_at ON news(published_at DESC);
CREATE INDEX idx_news_source ON news(source_name);

-- Instruments Table
CREATE TABLE instruments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    exchange VARCHAR(50),
    currency VARCHAR(3) DEFAULT 'TRY',
    current_price DECIMAL(18, 6),
    previous_close DECIMAL(18, 6),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_instrument_type CHECK (type IN ('CURRENCY', 'STOCK', 'BOND', 'FUND', 'VIOP', 'COMMODITY'))
);

CREATE INDEX idx_instruments_symbol ON instruments(symbol);
CREATE INDEX idx_instruments_type ON instruments(type);

-- Currency Rates Table
CREATE TABLE currency_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    currency_code VARCHAR(3) NOT NULL,
    currency_name VARCHAR(100),
    buying_rate DECIMAL(18, 6) NOT NULL,
    selling_rate DECIMAL(18, 6) NOT NULL,
    effective_buying_rate DECIMAL(18, 6),
    effective_selling_rate DECIMAL(18, 6),
    source VARCHAR(20) NOT NULL,
    fetched_at TIMESTAMP NOT NULL,
    rate_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_rate_source CHECK (source IN ('TCMB', 'YAHOO_FINANCE', 'ALPHA_VANTAGE', 'BANK_API', 'MANUAL'))
);

CREATE INDEX idx_currency_rates_code ON currency_rates(currency_code);
CREATE INDEX idx_currency_rates_source ON currency_rates(source);
CREATE INDEX idx_currency_rates_fetched_at ON currency_rates(fetched_at DESC);

-- Price History Table
CREATE TABLE price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instrument_id UUID NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    open_price DECIMAL(18, 6),
    high_price DECIMAL(18, 6),
    low_price DECIMAL(18, 6),
    close_price DECIMAL(18, 6) NOT NULL,
    adj_close DECIMAL(18, 6),
    volume BIGINT,
    price_date DATE NOT NULL,
    UNIQUE(instrument_id, price_date)
);

CREATE INDEX idx_price_history_instrument ON price_history(instrument_id);
CREATE INDEX idx_price_history_date ON price_history(price_date DESC);
CREATE INDEX idx_price_history_instrument_date ON price_history(instrument_id, price_date);

-- Portfolios Table
CREATE TABLE portfolios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_portfolios_user ON portfolios(user_id);

-- Portfolio Items Table
CREATE TABLE portfolio_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id),
    quantity DECIMAL(18, 6) NOT NULL,
    purchase_price DECIMAL(18, 6) NOT NULL,
    purchase_date DATE NOT NULL,
    notes VARCHAR(500)
);

CREATE INDEX idx_portfolio_items_portfolio ON portfolio_items(portfolio_id);
CREATE INDEX idx_portfolio_items_instrument ON portfolio_items(instrument_id);
