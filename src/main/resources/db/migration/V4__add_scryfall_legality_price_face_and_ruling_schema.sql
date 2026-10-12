-- Scryfall legalities are stored in the existing card_legality table, keyed by
-- card printing and format. Add normalized storage for print prices, faces, and rulings.
DO $$
BEGIN
    IF to_regclass('public.cards') IS NULL THEN
        RAISE EXCEPTION 'Baseline schema missing: table "cards" not found. Provision schema baseline before running V4.';
    END IF;
END
$$;

CREATE TABLE card_prices (
    card_id INTEGER PRIMARY KEY REFERENCES cards(id) ON DELETE CASCADE,
    usd NUMERIC(12, 4) CHECK (usd IS NULL OR usd >= 0),
    usd_foil NUMERIC(12, 4) CHECK (usd_foil IS NULL OR usd_foil >= 0),
    usd_etched NUMERIC(12, 4) CHECK (usd_etched IS NULL OR usd_etched >= 0),
    eur NUMERIC(12, 4) CHECK (eur IS NULL OR eur >= 0),
    eur_foil NUMERIC(12, 4) CHECK (eur_foil IS NULL OR eur_foil >= 0),
    eur_etched NUMERIC(12, 4) CHECK (eur_etched IS NULL OR eur_etched >= 0),
    tix NUMERIC(12, 4) CHECK (tix IS NULL OR tix >= 0),
    price_observed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE card_faces (
    card_id INTEGER NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    face_index SMALLINT NOT NULL CHECK (face_index >= 0),
    name TEXT NOT NULL,
    mana_cost TEXT NOT NULL DEFAULT '',
    type_line TEXT,
    oracle_text TEXT,
    colors JSONB,
    color_indicator JSONB,
    power TEXT,
    toughness TEXT,
    loyalty TEXT,
    defense TEXT,
    image_uris JSONB,
    face_data JSONB NOT NULL CHECK (jsonb_typeof(face_data) = 'object'),
    PRIMARY KEY (card_id, face_index)
);

CREATE TABLE card_rulings (
    oracle_id UUID NOT NULL,
    published_at DATE NOT NULL,
    comment TEXT NOT NULL,
    source TEXT NOT NULL,
    PRIMARY KEY (oracle_id, published_at, comment, source)
);

CREATE INDEX ix_card_rulings_oracle_id ON card_rulings(oracle_id);
