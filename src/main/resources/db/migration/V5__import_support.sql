-- ============================================================================
-- V5 - Import support
-- ----------------------------------------------------------------------------
-- The Scryfall importer never deletes card rows: card_deck references them
-- with ON DELETE CASCADE. A printing that disappears from the bulk is marked
-- obsolete instead, and a later bulk can clear the mark if it comes back.
--
-- import_seen records the printings seen by the run in progress so the final
-- anti-join can find what is missing without keeping hundreds of thousands of
-- UUIDs in memory. It is UNLOGGED on purpose: the data is only meaningful to
-- the running importer, and if the process dies the import is simply repeated.
-- It cannot be TEMP because batches use pooled connections.
--
-- Design: docs/IMPORTER_PLAN.md
-- Decisions: docs/DECISION_LOG.md (D-026)
-- ============================================================================

ALTER TABLE cards ADD COLUMN is_obsolete BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_cards_obsolete ON cards (is_obsolete) WHERE is_obsolete;

CREATE UNLOGGED TABLE import_seen (
    run_id      UUID NOT NULL,
    scryfall_id UUID NOT NULL,
    PRIMARY KEY (run_id, scryfall_id)
);

COMMENT ON COLUMN cards.is_obsolete IS
    'TRUE when the printing was absent from the latest full Scryfall import; rows are never deleted.';
COMMENT ON TABLE import_seen IS
    'Scryfall printings seen by an importer run, used to mark cards.is_obsolete.';
