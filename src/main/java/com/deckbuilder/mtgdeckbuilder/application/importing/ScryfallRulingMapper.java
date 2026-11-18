package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Maps one Scryfall ruling object onto {@link ImportRuling}.
 *
 * <p>
 * A ruling without an oracle id, a valid publication date or a non-blank
 * comment cannot be stored and is skipped. {@code source} is normalised to
 * lower case; when Scryfall omits it, the ruling is attributed to
 * {@code scryfall}.
 */
public final class ScryfallRulingMapper {

	private static final String DEFAULT_SOURCE = "scryfall";

	private ScryfallRulingMapper() {
	}

	public static Optional<ImportRuling> toImportRuling(JsonNode ruling) {
		if (ruling == null || !ruling.isObject()) {
			return Optional.empty();
		}
		final UUID oracleId = uuid(ruling.get("oracle_id"));
		final LocalDate publishedAt = date(ruling.get("published_at"));
		final String comment = text(ruling.get("comment"));
		if (oracleId == null || publishedAt == null || comment == null) {
			return Optional.empty();
		}
		return Optional.of(new ImportRuling(oracleId, publishedAt, comment, source(ruling.get("source"))));
	}

	private static String source(JsonNode node) {
		final String source = text(node);
		return source == null ? DEFAULT_SOURCE : source.toLowerCase(Locale.ROOT);
	}

	private static UUID uuid(JsonNode node) {
		final String value = text(node);
		if (value == null) {
			return null;
		}
		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException ex) {
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
		}
		catch (DateTimeParseException ex) {
			return null;
		}
	}

	private static String text(JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		final String value = node.asText().trim();
		return value.isEmpty() ? null : value;
	}
}
