package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pure helpers that normalize Scryfall card fields into the shape required by
 * {@code cards}.
 *
 * <p>
 * Scryfall omits some fields for certain layouts, so the importer needs the
 * same fallbacks the previous script had: an unknown type line, an unknown card
 * type, mana value zero and a placeholder image. Keeping the rules here makes
 * them testable without JSON or a database.
 */
public final class CardShapeRules {

	public static final String UNKNOWN_TYPE_LINE = "Unknown";
	public static final String UNKNOWN_CARD_TYPE = "Unknown";
	public static final String MISSING_IMAGE_URL = "https://example.com/missing.jpg";

	private static final String TYPE_SEPARATOR = "\u2014";

	private static final Set<String> SUPERTYPES = Set.of(
			"Basic", "Elite", "Legendary", "Ongoing", "Snow", "World");

	private static final Set<String> CARD_TYPES = Set.of(
			"Land", "Creature", "Artifact", "Enchantment", "Planeswalker",
			"Instant", "Sorcery", "Tribal", "Battle", "Kindred");

	private CardShapeRules() {
	}

	/**
	 * Components of a Scryfall type line, for example
	 * {@code "Legendary Creature — Goblin Wizard"}.
	 */
	public record TypeParts(String supertype, String type, String subtype) {
	}

	/**
	 * Splits a type line into supertype, card type and subtype. Unknown tokens
	 * are ignored, matching the previous importer.
	 */
	public static TypeParts parseTypeLine(String typeLine) {
		if (typeLine == null || typeLine.isBlank()) {
			return new TypeParts("", "", "");
		}
		final String[] halves = typeLine.split(TYPE_SEPARATOR, 2);
		final String left = halves[0].trim();
		final String subtype = halves.length > 1 ? halves[1].trim() : "";

		final List<String> supertypes = new ArrayList<>();
		final List<String> types = new ArrayList<>();
		for (final String token : left.split("\\s+")) {
			final String supertype = canonical(SUPERTYPES, token);
			if (supertype != null) {
				supertypes.add(supertype);
				continue;
			}
			final String type = canonical(CARD_TYPES, token);
			if (type != null) {
				types.add(type);
			}
		}
		return new TypeParts(String.join(" ", supertypes), String.join(" ", types), subtype);
	}

	/**
	 * {@code cards.type_line} is required, so a missing value becomes
	 * {@code "Unknown"}.
	 */
	public static String typeLine(String rawTypeLine) {
		return rawTypeLine == null || rawTypeLine.isBlank() ? UNKNOWN_TYPE_LINE : rawTypeLine;
	}

	/**
	 * {@code cards.card_type} is required and comes from the parsed type line.
	 */
	public static String cardType(TypeParts parts) {
		return parts.type().isBlank() ? UNKNOWN_CARD_TYPE : parts.type();
	}

	/**
	 * Converts Scryfall's decimal mana value to the integer column. Missing
	 * values become zero; fractions are truncated like the previous script's
	 * {@code int()} call.
	 */
	public static int manaValue(Number cmc) {
		return cmc == null ? 0 : cmc.intValue();
	}

	/**
	 * Image URL with the historical placeholder for layouts that omit it.
	 */
	public static String imageUrl(String normalImageUrl) {
		return normalImageUrl == null || normalImageUrl.isBlank() ? MISSING_IMAGE_URL : normalImageUrl;
	}

	/**
	 * Basic lands are exempt from the format copy limit.
	 */
	public static boolean isBasicLand(TypeParts parts) {
		return hasToken(parts.supertype(), "Basic") && hasToken(parts.type(), "Land");
	}

	private static String canonical(Set<String> values, String token) {
		for (final String value : values) {
			if (value.equalsIgnoreCase(token)) {
				return value;
			}
		}
		return null;
	}

	private static boolean hasToken(String value, String token) {
		return (" " + value + " ").contains(" " + token + " ");
	}
}
