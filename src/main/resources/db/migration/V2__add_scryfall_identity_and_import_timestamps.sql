-- Adds Scryfall identity columns and ingestion traceability timestamps to cards.
-- V2 is used so existing databases baselined at version 1 still execute this migration.

ALTER TABLE IF EXISTS cards
    ADD COLUMN IF NOT EXISTS scryfall_id UUID,
    ADD COLUMN IF NOT EXISTS oracle_id UUID,
    ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE IF EXISTS cards
    ALTER COLUMN imported_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF to_regclass('public.cards') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS ux_cards_scryfall_id
            ON cards (scryfall_id)
            WHERE scryfall_id IS NOT NULL;

        CREATE INDEX IF NOT EXISTS ix_cards_oracle_id
            ON cards (oracle_id);

        CREATE INDEX IF NOT EXISTS ix_cards_updated_at
            ON cards (updated_at);
    END IF;
END
$$;

