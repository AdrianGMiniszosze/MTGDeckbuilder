-- Adds Scryfall identity columns and ingestion traceability timestamps to cards.
-- Requires baseline schema provisioning (cards table must already exist).
-- V2 is used so existing databases baselined at version 1 still execute this migration.

DO $$
BEGIN
    IF to_regclass('public.cards') IS NULL THEN
        RAISE EXCEPTION 'Baseline schema missing: table "cards" not found. Provision schema baseline before running V2.';
    END IF;
END
$$;

ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS scryfall_id UUID,
    ADD COLUMN IF NOT EXISTS oracle_id UUID,
    ADD COLUMN IF NOT EXISTS imported_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE cards
    ALTER COLUMN imported_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP;

UPDATE cards
SET imported_at = COALESCE(imported_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE imported_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE cards
    ALTER COLUMN imported_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_cards_scryfall_id
    ON cards (scryfall_id)
    WHERE scryfall_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_cards_oracle_id
    ON cards (oracle_id);

CREATE INDEX IF NOT EXISTS ix_cards_updated_at
    ON cards (updated_at);

CREATE OR REPLACE FUNCTION set_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_cards_updated_at ON cards;

CREATE TRIGGER trg_cards_updated_at
BEFORE UPDATE ON cards
FOR EACH ROW
EXECUTE FUNCTION set_cards_updated_at();

