-- ============================================================================
-- V2 — Seed formats
-- ----------------------------------------------------------------------------
-- Initial playable formats and their structural rules.
--
-- Card legality (banned/restricted) is NOT stored here: it lives in
-- card_legality and is imported from Scryfall. These rows only describe the
-- shape of a legal deck.
--
-- Draft and Brawl are intentionally absent: a future Limited/draft tool needs
-- its own logic and does not require a predefined playable format.
-- ============================================================================

INSERT INTO formats (format_name, description, min_deck_size, max_deck_size, max_sideboard_size, max_copies, singleton, requires_commander)
VALUES
    ('Standard',  'Constructed format using recent Standard-legal sets.', 60, NULL, 15, 4, FALSE, FALSE),
    ('Commander', 'Singleton 100-card format with a designated commander.', 100, 100, 0, 1, TRUE, TRUE),
    ('Modern',    'Constructed format using Modern-legal sets.', 60, NULL, 15, 4, FALSE, FALSE),
    ('Legacy',    'Eternal constructed format with a banned list and no restricted list.', 60, NULL, 15, 4, FALSE, FALSE),
    ('Vintage',   'Eternal constructed format with both banned and restricted lists.', 60, NULL, 15, 4, FALSE, FALSE),
    ('Pauper',    'Constructed format restricted to common-rarity cards.', 60, NULL, 15, 4, FALSE, FALSE)
ON CONFLICT (format_name) DO NOTHING;
