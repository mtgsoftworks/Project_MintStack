-- V12__update_constraints_for_crypto_and_finnhub.sql
-- Update check constraints to support CRYPTO instrument type and FINNHUB rate source

-- Drop and recreate instrument type check constraint to include CRYPTO and INDEX
ALTER TABLE instruments DROP CONSTRAINT IF EXISTS chk_instrument_type;

ALTER TABLE instruments 
ADD CONSTRAINT chk_instrument_type 
CHECK (type IN ('CURRENCY', 'STOCK', 'BOND', 'FUND', 'VIOP', 'CRYPTO', 'COMMODITY', 'INDEX'));

-- Drop and recreate rate source check constraint to include FINNHUB
ALTER TABLE currency_rates DROP CONSTRAINT IF EXISTS chk_rate_source;

ALTER TABLE currency_rates 
ADD CONSTRAINT chk_rate_source 
CHECK (source IN ('TCMB', 'YAHOO_FINANCE', 'ALPHA_VANTAGE', 'FINNHUB', 'BANK_API', 'MANUAL'));
