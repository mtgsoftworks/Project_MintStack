-- V8__add_user_api_configs.sql
-- User API configuration table (for external providers)

CREATE TABLE IF NOT EXISTS user_api_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    api_key VARCHAR(500) NOT NULL,
    secret_key VARCHAR(500),
    base_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_user_api_provider CHECK (provider IN ('YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB', 'TCMB', 'OTHER'))
);

CREATE INDEX IF NOT EXISTS idx_user_api_configs_user_id ON user_api_configs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_api_configs_provider ON user_api_configs(provider);
