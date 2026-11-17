package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import com.deckbuilder.mtgdeckbuilder.application.importing.ImportCard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Writes card rows for the importer.
 *
 * <p>
 * Plain SQL instead of Hibernate: the import handles hundreds of thousands of
 * rows and must not pay for an identity map or managed collections (D-024).
 * The upsert is keyed by {@code scryfall_id}; PostgreSQL's {@code xmax = 0}
 * tells the caller whether the row was inserted or updated without comparing
 * contents.
 */
@Repository
public class CardImportDao {

	private static final String UPSERT_CARD = """
			INSERT INTO cards (
			    scryfall_id, oracle_id, card_name, mana_cost, cmc, color_identity,
			    type_line, card_type, card_supertype, rarity, card_text, flavor_text,
			    power, toughness, copies_limit, is_basic_land, image_url, foil,
			    game_changer, language, collector_number, promo, variation
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
			ON CONFLICT (scryfall_id) DO UPDATE SET
			    oracle_id = EXCLUDED.oracle_id,
			    card_name = EXCLUDED.card_name,
			    mana_cost = EXCLUDED.mana_cost,
			    cmc = EXCLUDED.cmc,
			    color_identity = EXCLUDED.color_identity,
			    type_line = EXCLUDED.type_line,
			    card_type = EXCLUDED.card_type,
			    card_supertype = EXCLUDED.card_supertype,
			    rarity = EXCLUDED.rarity,
			    card_text = EXCLUDED.card_text,
			    flavor_text = EXCLUDED.flavor_text,
			    power = EXCLUDED.power,
			    toughness = EXCLUDED.toughness,
			    copies_limit = EXCLUDED.copies_limit,
			    is_basic_land = EXCLUDED.is_basic_land,
			    image_url = EXCLUDED.image_url,
			    foil = EXCLUDED.foil,
			    game_changer = EXCLUDED.game_changer,
			    language = EXCLUDED.language,
			    collector_number = EXCLUDED.collector_number,
			    promo = EXCLUDED.promo,
			    variation = EXCLUDED.variation,
			    updated_at = now()
			RETURNING id, (xmax = 0) AS inserted
			""";

	private final JdbcTemplate jdbcTemplate;

	public CardImportDao(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public UpsertResult upsert(ImportCard card) {
		return jdbcTemplate.queryForObject(UPSERT_CARD, (resultSet, rowNumber) -> new UpsertResult(
				resultSet.getLong("id"),
				resultSet.getBoolean("inserted")),
				card.scryfallId(),
				card.oracleId(),
				card.name(),
				card.manaCost(),
				card.cmc(),
				card.colorIdentity(),
				card.typeLine(),
				card.cardType(),
				card.cardSupertype(),
				card.rarity(),
				card.cardText(),
				card.flavorText(),
				card.power(),
				card.toughness(),
				card.copiesLimit(),
				card.basicLand(),
				card.imageUrl(),
				card.foil(),
				card.gameChanger(),
				card.language(),
				card.collectorNumber(),
				card.promo(),
				card.variation());
	}

	public record UpsertResult(long cardId, boolean inserted) {
	}
}
