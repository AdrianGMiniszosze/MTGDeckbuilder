DROP TABLE IF EXISTS card_tag CASCADE;
DROP TABLE IF EXISTS card_deck CASCADE;
DROP TABLE IF EXISTS card_legality CASCADE;
DROP TABLE IF EXISTS card_subtypes CASCADE;
DROP TABLE IF EXISTS card_keywords CASCADE;
DROP TABLE IF EXISTS card_color_identity CASCADE;
DROP TABLE IF EXISTS card_colors CASCADE;
DROP TABLE IF EXISTS decks CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS tags CASCADE;
DROP TABLE IF EXISTS formats CASCADE;
DROP TABLE IF EXISTS cards CASCADE;
DROP TABLE IF EXISTS sets CASCADE;

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE sets (
    id SERIAL PRIMARY KEY,
    name TEXT UNIQUE NOT NULL
);

CREATE TABLE cards (
    id SERIAL PRIMARY KEY,
    card_name TEXT NOT NULL,
    mana_cost TEXT NOT NULL,
    cmc INTEGER NOT NULL,
    color_identity TEXT NOT NULL,
    type_line TEXT NOT NULL,
    card_type TEXT NOT NULL,
    card_supertype TEXT,
    rarity TEXT NOT NULL,
    card_text TEXT NOT NULL,
    flavor_text TEXT,
    power TEXT,
    toughness TEXT,
    unlimited_copies BOOLEAN NOT NULL DEFAULT FALSE,
    image_url TEXT NOT NULL,
    foil BOOLEAN NOT NULL DEFAULT FALSE,
    game_changer BOOLEAN NOT NULL DEFAULT FALSE,
    related_card INTEGER REFERENCES cards(id),
    language TEXT NOT NULL,
    embedding VECTOR(1536),
    archetype TEXT,
    card_set INTEGER REFERENCES sets(id),
    collector_number TEXT NOT NULL DEFAULT 'unknown',
    promo BOOLEAN NOT NULL DEFAULT FALSE,
    variation BOOLEAN NOT NULL DEFAULT FALSE
);

-- Card colors table (many-to-many for card colors)
CREATE TABLE card_colors (
    card_id INTEGER NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    color VARCHAR(1) NOT NULL CHECK (color IN ('W', 'U', 'B', 'R', 'G')),
    PRIMARY KEY (card_id, color)
);

-- Card color identity table (many-to-many for color identity)
CREATE TABLE card_color_identity (
    card_id INTEGER NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    color VARCHAR(1) NOT NULL CHECK (color IN ('W', 'U', 'B', 'R', 'G')),
    PRIMARY KEY (card_id, color)
);

-- Card keywords table (many-to-many for keywords)
CREATE TABLE card_keywords (
    card_id INTEGER NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    keyword TEXT NOT NULL,
    PRIMARY KEY (card_id, keyword)
);

-- Card subtypes table (many-to-many for subtypes)
CREATE TABLE card_subtypes (
    card_id INTEGER NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    subtype TEXT NOT NULL,
    PRIMARY KEY (card_id, subtype)
);

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    registration_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hashed_password TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    country TEXT NOT NULL
);

CREATE TABLE formats (
    id SERIAL PRIMARY KEY,
    format_name TEXT NOT NULL UNIQUE,
    deck_size INTEGER NOT NULL
);

CREATE TABLE decks (
    id SERIAL PRIMARY KEY,
    deck_name TEXT NOT NULL,
    creation_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modification TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deck_type TEXT NOT NULL CHECK (deck_type IN ('main', 'sideboard', 'maybeboard')),
    description TEXT,
    private BOOLEAN NOT NULL DEFAULT TRUE,
    tournament TEXT,
    share_url TEXT UNIQUE,
    parent_deck_id INTEGER REFERENCES decks(id) ON DELETE CASCADE,
    format INTEGER REFERENCES formats(id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE tags (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE card_legality (
    card_id INTEGER REFERENCES cards(id) ON DELETE CASCADE,
    format_id INTEGER REFERENCES formats(id) ON DELETE CASCADE,
    legality_status TEXT NOT NULL,
    PRIMARY KEY (card_id, format_id)
);

CREATE TABLE card_deck (
    id SERIAL PRIMARY KEY,
    card_id INTEGER REFERENCES cards(id) ON DELETE CASCADE,
    deck_id INTEGER REFERENCES decks(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    section VARCHAR(20) NOT NULL DEFAULT 'main' CHECK (section IN ('main', 'sideboard', 'maybeboard')),
    UNIQUE (card_id, deck_id, section)
);

CREATE TABLE card_tag (
    card_id INTEGER REFERENCES cards(id) ON DELETE CASCADE,
    tag_id INTEGER REFERENCES tags(id) ON DELETE CASCADE,
    weight FLOAT NOT NULL,
    confidence FLOAT NOT NULL DEFAULT 1.0,
    source TEXT DEFAULT 'manual',
    model_version TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (card_id, tag_id)
);

-- Unique Constraints
ALTER TABLE tags ADD CONSTRAINT unique_tag_name UNIQUE (name);
ALTER TABLE formats ADD CONSTRAINT unique_format_name UNIQUE (format_name);
ALTER TABLE sets ADD CONSTRAINT unique_set_name UNIQUE (name);
ALTER TABLE cards ADD CONSTRAINT unique_card_variant UNIQUE (card_name, card_set, collector_number);

-- Indexes
CREATE INDEX idx_card_name ON cards(card_name);
CREATE INDEX idx_collector_number ON cards(card_set, collector_number);
CREATE INDEX idx_card_tag_tag_id ON card_tag(tag_id);
CREATE INDEX idx_card_deck_deck_id ON card_deck(deck_id);
CREATE INDEX idx_card_legality_format_id ON card_legality(format_id);
CREATE INDEX idx_card_colors_color ON card_colors(color);
CREATE INDEX idx_card_color_identity_color ON card_color_identity(color);
CREATE INDEX idx_card_keywords_keyword ON card_keywords(keyword);
CREATE INDEX idx_card_subtypes_subtype ON card_subtypes(subtype);

CREATE OR REPLACE FUNCTION check_card_quantity()
RETURNS TRIGGER AS $$
DECLARE
    deck_format_id INTEGER;
    legality_status TEXT;
    card_type_name TEXT;
    card_super_type_name TEXT;
    unlimited_copies_flag BOOLEAN;
    max_quantity_single_card INTEGER;
    current_total_cards INTEGER;
    max_deck_size INTEGER;
BEGIN
    -- Use the section from card_deck (NEW.section) instead of deck_type
    -- Skip validation for maybeboard cards
    IF NEW.section = 'maybeboard' THEN
        RETURN NEW;
    END IF;

    -- Get the deck's format
    SELECT format INTO deck_format_id
    FROM decks
    WHERE id = NEW.deck_id;

    SELECT legality_status INTO legality_status
    FROM card_legality
    WHERE card_id = NEW.card_id AND format_id = deck_format_id;

    IF legality_status = 'banned' THEN
        RAISE EXCEPTION 'Card is banned in the % format.', (SELECT format_name FROM formats WHERE id = deck_format_id);
    END IF;

    SELECT card_type, card_supertype, unlimited_copies INTO card_type_name, card_super_type_name, unlimited_copies_flag
    FROM cards
    WHERE id = NEW.card_id;

    IF unlimited_copies_flag THEN
        max_quantity_single_card := 9999;
    ELSIF card_type_name = 'Land' AND card_super_type_name = 'Basic' THEN
        max_quantity_single_card := 9999;
    ELSIF legality_status = 'restricted' THEN
        max_quantity_single_card := 1;
    ELSIF (SELECT format_name FROM formats WHERE id = deck_format_id) = 'Commander' THEN
        max_quantity_single_card := 1;
    ELSE
        max_quantity_single_card := 4;
    END IF;

    IF NEW.quantity IS NULL THEN
        RAISE EXCEPTION 'Quantity cannot be null for card %', NEW.card_id;
    END IF;

    IF NEW.quantity > max_quantity_single_card THEN
        RAISE EXCEPTION 'Card with id %s quantity limit exceeded. Max allowed is %.', NEW.card_id, max_quantity_single_card;
    END IF;

    SELECT SUM(quantity) INTO current_total_cards
    FROM card_deck
    WHERE deck_id = NEW.deck_id AND card_id != NEW.card_id AND section = NEW.section;

    IF TG_OP = 'UPDATE' THEN
        SELECT quantity INTO current_total_cards
        FROM card_deck
        WHERE deck_id = OLD.deck_id AND card_id = OLD.card_id AND section = OLD.section;
    END IF;

    -- Set max deck size based on section
    IF NEW.section = 'sideboard' THEN
        max_deck_size := 15;
    ELSE
        -- For main deck, use format's deck size limit
        SELECT deck_size INTO max_deck_size
        FROM formats
        WHERE id = deck_format_id;
    END IF;

    IF (COALESCE(current_total_cards, 0) + NEW.quantity) > max_deck_size THEN
        RAISE EXCEPTION 'Deck section "%" size limit exceeded. Max allowed is %.', NEW.section, max_deck_size;
    END IF;

    UPDATE decks SET last_modification = CURRENT_TIMESTAMP WHERE id = NEW.deck_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER enforce_card_quantity_limit
BEFORE INSERT OR UPDATE ON card_deck
FOR EACH ROW
EXECUTE FUNCTION check_card_quantity();