-- Backfill timestamps for rows in databases that already applied V2.
UPDATE cards
SET imported_at = COALESCE(imported_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, CURRENT_TIMESTAMP)
WHERE imported_at IS NULL
   OR updated_at IS NULL;

ALTER TABLE cards
    ALTER COLUMN imported_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;
