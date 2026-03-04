-- Allow same symbol to exist for both real and simulated instruments.
-- Legacy schema had UNIQUE(symbol), which blocks simulation inserts when real rows exist.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'instruments_symbol_key'
          AND conrelid = 'instruments'::regclass
    ) THEN
        ALTER TABLE instruments DROP CONSTRAINT instruments_symbol_key;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_instruments_symbol_simulated'
          AND conrelid = 'instruments'::regclass
    ) THEN
        ALTER TABLE instruments
            ADD CONSTRAINT uk_instruments_symbol_simulated UNIQUE (symbol, is_simulated);
    END IF;
END $$;
