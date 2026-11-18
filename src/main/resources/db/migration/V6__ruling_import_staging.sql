-- ============================================================================
-- V6 - Ruling import staging
-- ----------------------------------------------------------------------------
-- The rulings bulk has no ruling id: a ruling is identified only by
-- (oracle_id, published_at, comment, source). To mirror the current file, the
-- importer stages every ruling of the run, deletes card_rulings rows that are
-- not in the staging table and inserts the rest. That removes corrected and
-- retired rulings without needing to know which one Scryfall edited.
--
-- UNLOGGED on purpose: the data is only meaningful to the running importer and
-- is rebuilt if the process dies. It cannot be TEMP because batches use pooled
-- connections (same reason as import_seen in V5).
--
-- Design: docs/IMPORTER_PLAN.md
-- Decisions: docs/DECISION_LOG.md (D-030, D-032, D-033)
-- ============================================================================

CREATE UNLOGGED TABLE import_ruling (
    run_id       UUID NOT NULL,
    oracle_id    UUID NOT NULL,
    published_at DATE NOT NULL,
    comment      TEXT NOT NULL,
    source       TEXT NOT NULL,
    PRIMARY KEY (run_id, oracle_id, published_at, comment, source)
);

COMMENT ON TABLE import_ruling IS
    'Rulings seen by an importer run; card_rulings is mirrored from this table.';
