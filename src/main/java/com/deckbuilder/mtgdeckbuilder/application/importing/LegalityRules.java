package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Translates Scryfall legality entries into the format rows seeded by Flyway.
 *
 * <p>
 * Scryfall uses lowercase keys ({@code standard}, {@code commander}, …) and
 * lowercase statuses. Keys without a seeded format are ignored on purpose: the
 * card is reported as {@code LEGALITY_UNKNOWN} instead of failing the import.
 */
public final class LegalityRules {

	private static final Map<String, String> FORMATS;

	static {
		final Map<String, String> formats = new LinkedHashMap<>();
		formats.put("standard", "Standard");
		formats.put("modern", "Modern");
		formats.put("legacy", "Legacy");
		formats.put("vintage", "Vintage");
		formats.put("pauper", "Pauper");
		formats.put("commander", "Commander");
		FORMATS = Collections.unmodifiableMap(formats);
	}

	private static final Set<String> STATUSES = Set.of("legal", "not_legal", "restricted", "banned");

	private LegalityRules() {
	}

	/**
	 * Format name for a Scryfall legality key, or empty when no seeded format
	 * matches.
	 */
	public static Optional<String> formatName(String scryfallKey) {
		if (scryfallKey == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(FORMATS.get(scryfallKey.trim().toLowerCase(Locale.ROOT)));
	}

	/**
	 * Normalized status accepted by {@code card_legality.legality_status}, or
	 * empty when Scryfall sends a value the schema does not know.
	 */
	public static Optional<String> status(String rawStatus) {
		if (rawStatus == null) {
			return Optional.empty();
		}
		final String normalized = rawStatus.trim().toLowerCase(Locale.ROOT);
		return STATUSES.contains(normalized) ? Optional.of(normalized) : Optional.empty();
	}
}
