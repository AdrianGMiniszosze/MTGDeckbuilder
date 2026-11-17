package com.deckbuilder.mtgdeckbuilder.application.importing;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps Scryfall rule text onto {@code cards.copies_limit}.
 *
 * <p>
 * Scryfall exposes no copy-limit field, so the value is derived from the deck
 * construction sentence. A bare "up to N cards named" also appears in tutoring
 * effects and must not cap the deck, which is why the prefix is part of the
 * pattern.
 *
 * <p>
 * Result contract: {@code null} defers to the format, {@code 0} means any
 * number of copies, and a positive value is the exact cap.
 */
public final class CopyLimitRules {

	private static final Pattern ANY_NUMBER = Pattern.compile(
			"a deck can have any number of cards named", Pattern.CASE_INSENSITIVE);

	private static final Pattern UP_TO_NUMBER = Pattern.compile(
			"a deck can have up to (one|two|three|four|five|six|seven|eight|nine|ten|\\d+) cards? named",
			Pattern.CASE_INSENSITIVE);

	private static final Map<String, Integer> NUMBER_WORDS = Map.of(
			"one", 1, "two", 2, "three", 3, "four", 4, "five", 5,
			"six", 6, "seven", 7, "eight", 8, "nine", 9, "ten", 10);

	private CopyLimitRules() {
	}

	/**
	 * Derives the copy limit from a card's rules text.
	 */
	public static Integer copiesLimit(String oracleText) {
		if (oracleText == null || oracleText.isBlank()) {
			return null;
		}
		if (ANY_NUMBER.matcher(oracleText).find()) {
			return 0;
		}
		final Matcher matcher = UP_TO_NUMBER.matcher(oracleText);
		if (!matcher.find()) {
			return null;
		}
		final String token = matcher.group(1).toLowerCase(Locale.ROOT);
		if (!token.chars().allMatch(Character::isDigit)) {
			return NUMBER_WORDS.get(token);
		}
		try {
			final int limit = Integer.parseInt(token);
			return limit > 0 ? limit : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
