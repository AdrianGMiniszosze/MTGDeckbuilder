package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Maps one Scryfall card object onto {@link ImportCard}.
 *
 * <p>
 * Tokens, emblems and art series are not playable printings, so they are
 * discarded before looking at faces. Missing optional fields get the same
 * fallbacks the previous script had; a row without a printing id or a name
 * cannot be upserted and is skipped.
 */
public final class ScryfallCardMapper {

	private static final Set<String> SKIPPED_LAYOUTS = Set.of("token", "emblem", "art_series");

	private ScryfallCardMapper() {
	}

	public static Optional<ImportCard> toImportCard(JsonNode card) {
		if (card == null || !card.isObject() || SKIPPED_LAYOUTS.contains(card.path("layout").asText())) {
			return Optional.empty();
		}
		final UUID scryfallId = uuid(card.get("id"));
		final String name = text(card.get("name"));
		if (scryfallId == null || name == null) {
			return Optional.empty();
		}

		final String oracleText = textOr(card.get("oracle_text"), "");
		final CardShapeRules.TypeParts parts = CardShapeRules.parseTypeLine(text(card.get("type_line")));
		final String supertype = parts.supertype().isBlank() ? null : parts.supertype();

		return Optional.of(new ImportCard(
				scryfallId,
				uuid(card.get("oracle_id")),
				name,
				text(card.get("mana_cost")),
				manaValue(card.get("cmc")),
				colorIdentity(card.get("color_identity")),
				CardShapeRules.typeLine(text(card.get("type_line"))),
				CardShapeRules.cardType(parts),
				supertype,
				textOr(card.get("rarity"), "common"),
				oracleText,
				text(card.get("flavor_text")),
				text(card.get("power")),
				text(card.get("toughness")),
				CopyLimitRules.copiesLimit(oracleText),
				CardShapeRules.isBasicLand(parts),
				imageUrl(card),
				bool(card, "foil"),
				bool(card, "game_changer"),
				language(card),
				textOr(card.get("collector_number"), "unknown"),
				bool(card, "promo"),
				bool(card, "variation")));
	}

	private static int manaValue(JsonNode node) {
		return CardShapeRules.manaValue(node != null && node.isNumber() ? node.numberValue() : null);
	}

	private static String colorIdentity(JsonNode node) {
		if (node == null || !node.isArray() || node.isEmpty()) {
			return null;
		}
		return StreamSupport.stream(node.spliterator(), false)
			.map(JsonNode::asText)
			.map(value -> value.trim().toUpperCase(Locale.ROOT))
			.filter(value -> !value.isEmpty())
			.collect(Collectors.joining(","));
	}

	private static String imageUrl(JsonNode card) {
		String normal = normalImage(card.get("image_uris"));
		final JsonNode faces = card.path("card_faces");
		if (normal == null && faces.isArray() && !faces.isEmpty()) {
			normal = normalImage(faces.get(0).get("image_uris"));
		}
		return CardShapeRules.imageUrl(normal);
	}

	private static String normalImage(JsonNode imageUris) {
		return imageUris != null && imageUris.isObject() ? text(imageUris.get("normal")) : null;
	}

	private static String language(JsonNode card) {
		String language = text(card.get("lang"));
		if (language == null) {
			language = text(card.get("language"));
		}
		return language == null ? "en" : language;
	}

	private static boolean bool(JsonNode card, String field) {
		final JsonNode value = card.get(field);
		return value != null && value.asBoolean(false);
	}

	private static UUID uuid(JsonNode node) {
		final String value = text(node);
		if (value == null) {
			return null;
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static String text(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		final String value = node.asText();
		return value.isBlank() ? null : value;
	}

	private static String textOr(JsonNode node, String fallback) {
		final String value = text(node);
		return value == null ? fallback : value;
	}
}
