-- ============================================================================
-- V4 - Commander
-- ----------------------------------------------------------------------------
-- The commander is a card that is already part of the deck: it lists inside
-- the 100 and stays in the `main` section. This table only marks which rows of
-- card_deck hold that role, so the deck keeps a single source of truth for its
-- contents.
--
-- Two rows are allowed for partner-style pairs. Whether the pair is legal is
-- policy and lives in Java (`CommanderRules`), not in SQL.
--
-- Design: docs/DECK_VALIDATION_MODEL.md
-- Decision: docs/DECISION_LOG.md (D-012, D-021)
-- ============================================================================

CREATE TABLE deck_commander (
    deck_id BIGINT NOT NULL REFERENCES decks(id) ON DELETE CASCADE,
    card_id BIGINT NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    PRIMARY KEY (deck_id, card_id)
);

-- Supporting lookup for "which decks use this card as commander".
CREATE INDEX idx_deck_commander_card_id ON deck_commander (card_id);

COMMENT ON TABLE deck_commander IS
    'Cards from card_deck (main) that act as the deck commander; at most two for partner pairs.';
COMMENT ON COLUMN deck_commander.deck_id IS
    'Deck that declares the commander.';
COMMENT ON COLUMN deck_commander.card_id IS
    'Card marked as commander. Must also exist in card_deck with section = main.';
