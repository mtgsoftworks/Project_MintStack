-- BIST DataStore is the verified public source for BIST debt securities and VIOP bulletins.

ALTER TABLE user_api_configs DROP CONSTRAINT IF EXISTS chk_user_api_provider;
ALTER TABLE user_api_configs
    ADD CONSTRAINT chk_user_api_provider
    CHECK (provider IN (
        'YAHOO_FINANCE',
        'ALPHA_VANTAGE',
        'FINNHUB',
        'TCMB',
        'TEFAS',
        'BIST_DATASTORE',
        'FINTABLES',
        'RSS',
        'LLM_ENRICHMENT',
        'OTHER'
    ));

INSERT INTO runtime_settings (setting_key, setting_value, description, restart_required, updated_by)
VALUES
    (
        'app.external-api.bist-datastore.enabled',
        'true',
        'BIST public file integration for debt securities and VIOP daily bulletins.',
        false,
        'migration'
    )
ON CONFLICT (setting_key) DO UPDATE SET
    setting_value = EXCLUDED.setting_value,
    description = EXCLUDED.description,
    restart_required = EXCLUDED.restart_required,
    updated_by = EXCLUDED.updated_by,
    updated_at = CURRENT_TIMESTAMP;
