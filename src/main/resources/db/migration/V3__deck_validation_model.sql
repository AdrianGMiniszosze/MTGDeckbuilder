-- ============================================================================
-- V3 - Deck validation model
-- ----------------------------------------------------------------------------
-- Saving a deck never applies format rules; publishing it does. This
-- migration adds the state needed for that split and removes the SQL trigger
-- that used to enforce format policy on every write.
--
-- Design: docs/DECK_VALIDATION_MODEL.md
-- Decision: docs/DECISION_LOG.md (D-001, D-002, D-003, D-004)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Deck state
-- ----------------------------------------------------------------------------
ALTER TABLE decks ADD COLUMN status TEXT NOT NULL DEFAULT 'draft';
ALTER TABLE decks ADD COLUMN published_at TIMESTAMPTZ NULL;

ALTER TABLE decks
    ADD CONSTRAINT decks_status_check
    CHECK (status IN ('draft', 'published'));

-- A deck is published exactly when it has a publication timestamp.
ALTER TABLE decks
    ADD CONSTRAINT decks_published_at_check
    CHECK ((status = 'published') = (published_at IS NOT NULL));

CREATE INDEX idx_decks_status ON decks (status);

-- ----------------------------------------------------------------------------
-- Remove format policy from the database
-- ----------------------------------------------------------------------------
-- Structure stays (FKs, UNIQUE, CHECK). Format rules move to Java, where they
-- can express companions and where a single definition can be tested.
--
-- Dropped before touching cards.unlimited_copies: the function reads that
-- column and nothing should keep referring to it.
DROP TRIGGER IF EXISTS enforce_card_quantity_limit ON card_deck;
DROP FUNCTION IF EXISTS check_card_quantity();

-- ----------------------------------------------------------------------------
-- Formats: whether legality is tracked at all
-- ----------------------------------------------------------------------------
-- Casual decks must not report LEGALITY_UNKNOWN for every card just because
-- the format has no meaning for them.
ALTER TABLE formats ADD COLUMN tracks_legality BOOLEAN NOT NULL DEFAULT TRUE;

INSERT INTO formats (format_name, description, min_deck_size, max_deck_size,
                     max_sideboard_size, max_copies, singleton, requires_commander,
                     tracks_legality)
VALUES ('Casual',
        'No format rules: only data integrity. For decks played for fun.',
        0, NULL, NULL, 4, FALSE, FALSE, FALSE)
ON CONFLICT (format_name) DO NOTHING;

-- ----------------------------------------------------------------------------
-- Per-card copy limit
-- ----------------------------------------------------------------------------
-- NULL  -> the format decides (max_copies, singleton, restricted)
-- 0     -> no limit ("any number of cards named ...")
-- N > 0 -> exact cap that wins over the format ("up to seven cards named ...")
ALTER TABLE cards ADD COLUMN copies_limit INTEGER NULL;

ALTER TABLE cards
    ADD CONSTRAINT cards_copies_limit_check
    CHECK (copies_limit IS NULL OR copies_limit >= 0);

UPDATE cards SET copies_limit = 0 WHERE unlimited_copies = TRUE;

ALTER TABLE cards DROP COLUMN unlimited_copies;

COMMENT ON COLUMN cards.copies_limit IS
    'NULL = defer to format; 0 = no limit; N = exact cap that overrides the format.';

COMMENT ON COLUMN decks.status IS
    'draft = under construction, format rules not applied; published = playable, validated in Java.';
COMMENT ON COLUMN formats.tracks_legality IS
    'FALSE disables legality checks and LEGALITY_UNKNOWN warnings for the format.';
