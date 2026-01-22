-- Add simulation_configs table
CREATE TABLE IF NOT EXISTS simulation_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    volatility_level VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    update_interval_seconds INTEGER NOT NULL DEFAULT 5,
    market_trend VARCHAR(20) DEFAULT 'NEUTRAL',
    enable_random_events BOOLEAN DEFAULT TRUE,
    enable_market_hours BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Add is_simulated column to instruments table
ALTER TABLE instruments ADD COLUMN IF NOT EXISTS is_simulated BOOLEAN DEFAULT FALSE;

-- Update existing records to be non-simulated (real data)
UPDATE instruments SET is_simulated = FALSE WHERE is_simulated IS NULL;

-- Insert default simulation config if not exists
INSERT INTO simulation_configs (id, is_enabled, volatility_level, update_interval_seconds, market_trend, enable_random_events, enable_market_hours)
SELECT gen_random_uuid(), FALSE, 'MEDIUM', 5, 'NEUTRAL', TRUE, FALSE
WHERE NOT EXISTS (SELECT 1 FROM simulation_configs LIMIT 1);
