-- User Data Source Preferences
-- Allows users to select which provider to use for each data type

CREATE TABLE IF NOT EXISTS user_data_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    data_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    is_enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, data_type)
);

-- Data types enum reference:
-- CURRENCY_RATES - Döviz kurları
-- BIST_STOCKS - BIST hisse senetleri
-- US_STOCKS - ABD hisse senetleri
-- CRYPTO - Kripto paralar
-- NEWS - Haberler

CREATE INDEX idx_user_data_preferences_user ON user_data_preferences(user_id);
CREATE INDEX idx_user_data_preferences_type ON user_data_preferences(data_type);

-- Add trigger_on_save column to user_api_configs
ALTER TABLE user_api_configs ADD COLUMN IF NOT EXISTS last_triggered_at TIMESTAMP;
