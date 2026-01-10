-- V4__watchlist_alerts.sql
-- Watchlist and Price Alerts tables

-- Watchlists table
CREATE TABLE IF NOT EXISTS watchlists (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_watchlists_user_id ON watchlists(user_id);

-- Watchlist items table
CREATE TABLE IF NOT EXISTS watchlist_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    watchlist_id UUID NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(watchlist_id, instrument_id)
);

CREATE INDEX idx_watchlist_items_watchlist ON watchlist_items(watchlist_id);
CREATE INDEX idx_watchlist_items_instrument ON watchlist_items(instrument_id);

-- Price alerts table
CREATE TABLE IF NOT EXISTS price_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    instrument_id UUID NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    alert_type VARCHAR(20) NOT NULL,
    target_value DECIMAL(18,4) NOT NULL,
    current_value_at_creation DECIMAL(18,4),
    is_triggered BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    triggered_at TIMESTAMP,
    triggered_value DECIMAL(18,4),
    notification_sent BOOLEAN DEFAULT false,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_user_id ON price_alerts(user_id);
CREATE INDEX idx_alerts_instrument ON price_alerts(instrument_id);
CREATE INDEX idx_alerts_active ON price_alerts(is_active);

-- Add constraint for alert_type
ALTER TABLE price_alerts ADD CONSTRAINT chk_alert_type 
    CHECK (alert_type IN ('PRICE_ABOVE', 'PRICE_BELOW', 'PERCENT_UP', 'PERCENT_DOWN'));
