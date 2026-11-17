package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import com.deckbuilder.mtgdeckbuilder.application.importing.ImportCard;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportCardAttributes;
import com.deckbuilder.mtgdeckbuilder.application.importing.ImportFace;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Writes card rows for the importer.
 *
 * <p>
 * Plain SQL instead of Hibernate: the import handles hundreds of thousands of
 * rows and must not pay for an identity map or managed collections (D-024).
 * The upsert is keyed by {@code scryfall_id}; PostgreSQL's {@code xmax = 0}
 * tells the caller whether the row was inserted or updated without comparing
 * contents.
 *
 * <p>
 * Child rows are deleted and reinserted for every card (D-027), so a keyword
 * or legality that Scryfall retires does not survive the next import.
 *
 * <p>
 * Every upsert also records the printing in {@code import_seen} under the run
 * that saw it. The final anti-join marks printings absent from the run as
 * obsolete instead of deleting them: {@code card_deck} references cards with
 * {@code ON DELETE CASCADE} (D-026). A card seen again clears the mark.
 */
@Repository
public class CardImportDao {

	private static final String UPSERT_CARD = """
			INSERT INTO cards (
			    scryfall_id, oracle_id, card_name, mana_cost, cmc, color_identity,
			    type_line, card_type, card_supertype, rarity, card_text, flavor_text,
			    power, toughness, copies_limit, is_basic_land, image_url, foil,
			    game_changer, language, collector_number, promo, variation, set_id,
			    usd, usd_foil, usd_etched, eur, eur_foil, eur_etched, tix, price_observed_at
			) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
			    set_id = COALESCE(EXCLUDED.set_id, cards.set_id),
			    usd = EXCLUDED.usd,
			    usd_foil = EXCLUDED.usd_foil,
			    usd_etched = EXCLUDED.usd_etched,
			    eur = EXCLUDED.eur,
			    eur_foil = EXCLUDED.eur_foil,
			    eur_etched = EXCLUDED.eur_etched,
			    tix = EXCLUDED.tix,
			    price_observed_at = COALESCE(EXCLUDED.price_observed_at, cards.price_observed_at),
			    is_obsolete = FALSE,
			    updated_at = now()
			RETURNING id, (xmax = 0) AS inserted
			""";

	private static final String UPSERT_SET = """
			INSERT INTO sets (name, code, released_at, set_type)
			VALUES (?, ?, ?, ?)
			ON CONFLICT (name) DO UPDATE SET
			    code = COALESCE(sets.code, EXCLUDED.code),
			    released_at = COALESCE(sets.released_at, EXCLUDED.released_at),
			    set_type = COALESCE(sets.set_type, EXCLUDED.set_type),
			    updated_at = now()
			RETURNING id
			""";

	private static final String DELETE_COLORS = "DELETE FROM card_colors WHERE card_id = ?";
	private static final String DELETE_COLOR_IDENTITY = "DELETE FROM card_color_identity WHERE card_id = ?";
	private static final String DELETE_TYPES = "DELETE FROM card_types WHERE card_id = ?";
	private static final String DELETE_SUPERTYPES = "DELETE FROM card_supertypes WHERE card_id = ?";
	private static final String DELETE_SUBTYPES = "DELETE FROM card_subtypes WHERE card_id = ?";
	private static final String DELETE_KEYWORDS = "DELETE FROM card_keywords WHERE card_id = ?";
	private static final String DELETE_LEGALITIES = "DELETE FROM card_legality WHERE card_id = ?";
	private static final String DELETE_FACES = "DELETE FROM card_faces WHERE card_id = ?";

	private static final String INSERT_COLOR = "INSERT INTO card_colors (card_id, color) VALUES (?, ?)";
	private static final String INSERT_COLOR_IDENTITY =
			"INSERT INTO card_color_identity (card_id, color) VALUES (?, ?)";
	private static final String INSERT_TYPE = "INSERT INTO card_types (card_id, type) VALUES (?, ?)";
	private static final String INSERT_SUPERTYPE = "INSERT INTO card_supertypes (card_id, supertype) VALUES (?, ?)";
	private static final String INSERT_SUBTYPE = "INSERT INTO card_subtypes (card_id, subtype) VALUES (?, ?)";
	private static final String INSERT_KEYWORD = "INSERT INTO card_keywords (card_id, keyword) VALUES (?, ?)";
	private static final String INSERT_LEGALITY =
			"INSERT INTO card_legality (card_id, format_id, legality_status) VALUES (?, ?, ?)";

	private static final String INSERT_SEEN = """
			INSERT INTO import_seen (run_id, scryfall_id)
			VALUES (?, ?)
			ON CONFLICT DO NOTHING
			""";

	private static final String MARK_OBSOLETE = """
			UPDATE cards c
			   SET is_obsolete = TRUE, updated_at = now()
			 WHERE c.scryfall_id IS NOT NULL
			   AND c.is_obsolete = FALSE
			   AND NOT EXISTS (
			       SELECT 1 FROM import_seen s
			        WHERE s.run_id = ? AND s.scryfall_id = c.scryfall_id)
			""";

	private static final String DELETE_SEEN = "DELETE FROM import_seen WHERE run_id = ?";
	private static final String DELETE_OTHER_SEEN = "DELETE FROM import_seen WHERE run_id <> ?";
	private static final String COUNT_SEEN = "SELECT COUNT(*) FROM import_seen WHERE run_id = ?";

	private static final String INSERT_FACE = """
			INSERT INTO card_faces (
			    card_id, face_index, name, mana_cost, type_line, oracle_text,
			    colors, color_indicator, power, toughness, loyalty, defense,
			    image_uris, face_data
			) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
			""";

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;
	private final Map<String, Long> setIds = new HashMap<>();
	private Map<String, Long> formatIds;

	public CardImportDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	public UpsertResult upsert(ImportCard card, UUID runId) {
		final Long setId = resolveSet(card);
		final UpsertResult result = upsertCard(card, setId);
		replaceChildren(result.cardId(), card.attributes());
		jdbcTemplate.update(INSERT_SEEN, runId, card.scryfallId());
		return result;
	}

	/**
	 * Marks printings that were not seen by {@code runId} as obsolete.
	 * @throws IllegalStateException when the run saw nothing, because an empty or
	 * truncated file must never retire the whole catalogue.
	 */
	public int markObsolete(UUID runId) {
		if (countSeen(runId) == 0) {
			throw new IllegalStateException(
					"Refusing to mark cards obsolete: no seen rows for run " + runId);
		}
		return jdbcTemplate.update(MARK_OBSOLETE, runId);
	}

	public int countSeen(UUID runId) {
		final Integer count = jdbcTemplate.queryForObject(COUNT_SEEN, Integer.class, runId);
		return count == null ? 0 : count;
	}

	/** Removes this run's rows once the sweep has finished, including on failure. */
	public int clearSeen(UUID runId) {
		return jdbcTemplate.update(DELETE_SEEN, runId);
	}

	/** Removes rows left behind by runs that died before their cleanup. */
	public int clearOtherRuns(UUID runId) {
		return jdbcTemplate.update(DELETE_OTHER_SEEN, runId);
	}

	private UpsertResult upsertCard(ImportCard card, Long setId) {
		final Map<String, BigDecimal> prices = card.prices();
		final OffsetDateTime observedAt = prices.isEmpty() ? null : OffsetDateTime.now();
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
				card.variation(),
				setId,
				prices.get("usd"),
				prices.get("usd_foil"),
				prices.get("usd_etched"),
				prices.get("eur"),
				prices.get("eur_foil"),
				prices.get("eur_etched"),
				prices.get("tix"),
				observedAt);
	}

	private Long resolveSet(ImportCard card) {
		final String name = card.setName();
		if (name == null) {
			return null;
		}
		final Long cached = setIds.get(name);
		if (cached != null) {
			return cached;
		}
		final Long id = jdbcTemplate.queryForObject(
				UPSERT_SET, Long.class, name, card.setCode(), card.setReleasedAt(), card.setType());
		if (id != null) {
			setIds.put(name, id);
		}
		return id;
	}

	private void replaceChildren(long cardId, ImportCardAttributes attributes) {
		replace(cardId, DELETE_COLORS, INSERT_COLOR, attributes.colors());
		replace(cardId, DELETE_COLOR_IDENTITY, INSERT_COLOR_IDENTITY, attributes.colorIdentity());
		replace(cardId, DELETE_TYPES, INSERT_TYPE, attributes.types());
		replace(cardId, DELETE_SUPERTYPES, INSERT_SUPERTYPE, attributes.supertypes());
		replace(cardId, DELETE_SUBTYPES, INSERT_SUBTYPE, attributes.subtypes());
		replace(cardId, DELETE_KEYWORDS, INSERT_KEYWORD, attributes.keywords());
		replaceLegalities(cardId, attributes.legalities());
		replaceFaces(cardId, attributes.faces());
	}

	private void replace(long cardId, String deleteSql, String insertSql, List<String> values) {
		jdbcTemplate.update(deleteSql, cardId);
		for (final String value : values) {
			jdbcTemplate.update(insertSql, cardId, value);
		}
	}

	private void replaceLegalities(long cardId, Map<String, String> legalities) {
		jdbcTemplate.update(DELETE_LEGALITIES, cardId);
		for (final Map.Entry<String, String> legality : legalities.entrySet()) {
			final Long formatId = formats().get(legality.getKey());
			if (formatId != null) {
				jdbcTemplate.update(INSERT_LEGALITY, cardId, formatId, legality.getValue());
			}
		}
	}

	private Map<String, Long> formats() {
		if (formatIds == null) {
			final Map<String, Long> loaded = new HashMap<>();
			jdbcTemplate.queryForList("SELECT id, format_name FROM formats")
				.forEach(row -> loaded.put(
						(String) row.get("format_name"),
						((Number) row.get("id")).longValue()));
			formatIds = loaded;
		}
		return formatIds;
	}

	private void replaceFaces(long cardId, List<ImportFace> faces) {
		jdbcTemplate.update(DELETE_FACES, cardId);
		for (int index = 0; index < faces.size(); index++) {
			final ImportFace face = faces.get(index);
			jdbcTemplate.update(INSERT_FACE,
					cardId,
					index,
					face.name(),
					face.manaCost(),
					face.typeLine(),
					face.oracleText(),
					json(face.colors()),
					json(face.colorIndicator()),
					face.power(),
					face.toughness(),
					face.loyalty(),
					face.defense(),
					json(face.imageUris()),
					json(face.faceData()));
		}
	}

	private String json(Object value) {
		if (value == null || (value instanceof Collection<?> collection && collection.isEmpty())) {
			return null;
		}
		if (value instanceof JsonNode node) {
			return node.isNull() ? null : node.toString();
		}
		return objectMapper.valueToTree(value).toString();
	}

	public record UpsertResult(long cardId, boolean inserted) {
	}
}
