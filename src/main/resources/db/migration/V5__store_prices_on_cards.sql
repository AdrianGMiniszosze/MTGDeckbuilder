-- Prices are a small optional snapshot for each card printing; keep them with the card row.
ALTER TABLE cards
    ADD COLUMN usd NUMERIC(12, 4) CHECK (usd IS NULL OR usd >= 0),
    ADD COLUMN usd_foil NUMERIC(12, 4) CHECK (usd_foil IS NULL OR usd_foil >= 0),
    ADD COLUMN usd_etched NUMERIC(12, 4) CHECK (usd_etched IS NULL OR usd_etched >= 0),
    ADD COLUMN eur NUMERIC(12, 4) CHECK (eur IS NULL OR eur >= 0),
    ADD COLUMN eur_foil NUMERIC(12, 4) CHECK (eur_foil IS NULL OR eur_foil >= 0),
    ADD COLUMN eur_etched NUMERIC(12, 4) CHECK (eur_etched IS NULL OR eur_etched >= 0),
    ADD COLUMN tix NUMERIC(12, 4) CHECK (tix IS NULL OR tix >= 0),
    ADD COLUMN price_observed_at TIMESTAMP WITH TIME ZONE;

UPDATE cards AS card
SET usd = prices.usd,
    usd_foil = prices.usd_foil,
    usd_etched = prices.usd_etched,
    eur = prices.eur,
    eur_foil = prices.eur_foil,
    eur_etched = prices.eur_etched,
    tix = prices.tix,
    price_observed_at = prices.price_observed_at
FROM card_prices AS prices
WHERE prices.card_id = card.id;

DROP TABLE card_prices;
