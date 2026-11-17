package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
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
 *
 * <p>
 * Multi-face printings keep their text and types per face: the top-level
 * {@code oracle_text} is absent for them and the joined {@code type_line} is
 * ambiguous, so the card row gets the concatenated face text and the child
 * tables get the union of every face.
 */
public final class ScryfallCardMapper {

	private static final Set<String> SKIPPED_LAYOUTS = Set.of("token", "emblem", "art_series");

	private static final Set<String> VALID_COLORS = Set.of("W", "U", "B", "R", "G");

	private static final List<String> PRICE_FIELDS = List.of(
			"usd", "usd_foil", "usd_etched", "eur", "eur_foil", "eur_etched", "tix");

	private static final String FACE_TEXT_SEPARATOR = "\n\n";

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

		final JsonNode facesNode = card.path("card_faces");
		final List<ImportFace> faces = faces(card, facesNode);
		final String oracleText = oracleText(card, faces);
		final List<CardShapeRules.TypeParts> parts = typeParts(card, facesNode);
		final List<String> colors = colors(card);
		final List<String> colorIdentity = colorIdentity(card);
		final List<String> types = tokens(parts, CardShapeRules.TypeParts::type);
		final List<String> supertypes = tokens(parts, CardShapeRules.TypeParts::supertype);
		final List<String> subtypes = tokens(parts, CardShapeRules.TypeParts::subtype);

		final ImportCardAttributes attributes = new ImportCardAttributes(
				colors,
				colorIdentity,
				types,
				supertypes,
				subtypes,
				strings(card.get("keywords")),
				legalities(card.get("legalities")),
				faces);

		return Optional.of(new ImportCard(
				scryfallId,
				uuid(card.get("oracle_id")),
				name,
				text(card.get("mana_cost")),
				manaValue(card.get("cmc")),
				colorIdentityCsv(colorIdentity),
				typeLine(card, facesNode),
				types.isEmpty() ? CardShapeRules.UNKNOWN_CARD_TYPE : String.join(" ", types),
				supertypes.isEmpty() ? null : String.join(" ", supertypes),
				textOr(card.get("rarity"), "common"),
				oracleText,
				text(card.get("flavor_text")),
				text(card.get("power")),
				text(card.get("toughness")),
				CopyLimitRules.copiesLimit(oracleText),
				CardShapeRules.isBasicLand(CardShapeRules.parseTypeLine(text(card.get("type_line")))),
				imageUrl(card),
				bool(card, "foil"),
				bool(card, "game_changer"),
				language(card),
				textOr(card.get("collector_number"), "unknown"),
				bool(card, "promo"),
				bool(card, "variation"),
				text(card.get("set_name")),
				text(card.get("set")),
				date(card.get("released_at")),
				text(card.get("set_type")),
				prices(card.get("prices")),
				attributes));
	}

	private static int manaValue(JsonNode node) {
		return CardShapeRules.manaValue(node != null && node.isNumber() ? node.numberValue() : null);
	}

	private static String oracleText(JsonNode card, List<ImportFace> faces) {
		final String topLevel = text(card.get("oracle_text"));
		if (topLevel != null) {
			return topLevel;
		}
		final String joined = faces.stream()
			.map(ImportFace::oracleText)
			.filter(Objects::nonNull)
			.collect(Collectors.joining(FACE_TEXT_SEPARATOR));
		return joined.isEmpty() ? "" : joined;
	}

	private static String typeLine(JsonNode card, JsonNode facesNode) {
		String typeLine = text(card.get("type_line"));
		if (typeLine == null && facesNode.isArray() && !facesNode.isEmpty()) {
			typeLine = StreamSupport.stream(facesNode.spliterator(), false)
				.map(face -> text(face.get("type_line")))
				.filter(Objects::nonNull)
				.collect(Collectors.joining(" // "));
		}
		return CardShapeRules.typeLine(typeLine);
	}

	private static List<CardShapeRules.TypeParts> typeParts(JsonNode card, JsonNode facesNode) {
		final List<CardShapeRules.TypeParts> parts = new ArrayList<>();
		if (facesNode.isArray() && !facesNode.isEmpty()) {
			for (final JsonNode face : facesNode) {
				if (face.isObject()) {
					parts.add(CardShapeRules.parseTypeLine(text(face.get("type_line"))));
				}
			}
		}
		if (parts.stream().allMatch(part -> part.type().isBlank())) {
			parts.clear();
			parts.add(CardShapeRules.parseTypeLine(text(card.get("type_line"))));
		}
		return parts;
	}

	private static List<String> tokens(
			List<CardShapeRules.TypeParts> parts, Function<CardShapeRules.TypeParts, String> extractor) {
		final Set<String> values = new LinkedHashSet<>();
		for (final CardShapeRules.TypeParts part : parts) {
			for (final String token : extractor.apply(part).split("\\s+")) {
				if (!token.isBlank() && !token.equals("—") && !token.equals("//")) {
					values.add(token);
				}
			}
		}
		return List.copyOf(values);
	}

	private static List<String> colors(JsonNode card) {
		final List<String> topLevel = colorArray(card.get("colors"));
		if (!topLevel.isEmpty()) {
			return topLevel;
		}
		final Set<String> union = new LinkedHashSet<>();
		final JsonNode faces = card.path("card_faces");
		if (faces.isArray()) {
			for (final JsonNode face : faces) {
				union.addAll(colorArray(face.get("colors")));
			}
		}
		return List.copyOf(union);
	}

	private static List<String> colorIdentity(JsonNode card) {
		return colorArray(card.get("color_identity"));
	}

	private static List<String> colorArray(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		final Set<String> colors = new LinkedHashSet<>();
		for (final JsonNode value : node) {
			final String color = value.asText("").trim().toUpperCase(Locale.ROOT);
			if (VALID_COLORS.contains(color)) {
				colors.add(color);
			}
		}
		return List.copyOf(colors);
	}

	private static String colorIdentityCsv(List<String> colorIdentity) {
		return colorIdentity.isEmpty() ? null : String.join(",", colorIdentity);
	}

	private static List<ImportFace> faces(JsonNode card, JsonNode facesNode) {
		if (!facesNode.isArray() || facesNode.isEmpty()) {
			return List.of();
		}
		final List<ImportFace> faces = new ArrayList<>();
		for (final JsonNode face : facesNode) {
			if (!face.isObject()) {
				continue;
			}
			faces.add(new ImportFace(
					textOr(face.get("name"), textOr(card.get("name"), "Unknown")),
					textOr(face.get("mana_cost"), ""),
					text(face.get("type_line")),
					text(face.get("oracle_text")),
					colorArray(face.get("colors")),
					colorArray(face.get("color_indicator")),
					text(face.get("power")),
					text(face.get("toughness")),
					text(face.get("loyalty")),
					text(face.get("defense")),
					face.get("image_uris"),
					face));
		}
		return List.copyOf(faces);
	}

	private static List<String> strings(JsonNode node) {
		if (node == null || !node.isArray()) {
			return List.of();
		}
		final Set<String> values = new LinkedHashSet<>();
		for (final JsonNode value : node) {
			final String text = text(value);
			if (text != null) {
				values.add(text);
			}
		}
		return List.copyOf(values);
	}

	private static Map<String, String> legalities(JsonNode node) {
		if (node == null || !node.isObject()) {
			return Map.of();
		}
		final Map<String, String> legalities = new LinkedHashMap<>();
		node.fields().forEachRemaining(entry -> LegalityRules
			.formatName(entry.getKey())
			.flatMap(format -> LegalityRules.status(entry.getValue().asText())
				.map(status -> Map.entry(format, status)))
			.ifPresent(legality -> legalities.put(legality.getKey(), legality.getValue())));
		return Collections.unmodifiableMap(legalities);
	}

	private static Map<String, BigDecimal> prices(JsonNode node) {
		if (node == null || !node.isObject()) {
			return Map.of();
		}
		final Map<String, BigDecimal> prices = new LinkedHashMap<>();
		for (final String field : PRICE_FIELDS) {
			final BigDecimal price = decimal(node.get(field));
			if (price != null) {
				prices.put(field, price);
			}
		}
		return Collections.unmodifiableMap(prices);
	}

	private static BigDecimal decimal(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		final String value = node.asText().trim();
		if (value.isEmpty()) {
			return null;
		}
		try {
			final BigDecimal price = new BigDecimal(value);
			return price.signum() < 0 ? null : price;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static LocalDate date(JsonNode node) {
		final String value = text(node);
		if (value == null) {
			return null;
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException ex) {
			return null;
		}
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
