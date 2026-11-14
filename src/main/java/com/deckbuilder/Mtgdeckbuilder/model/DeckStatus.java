package com.deckbuilder.mtgdeckbuilder.model;

import java.util.Arrays;

/**
 * Lifecycle state of a deck.
 *
 * <p>
 * {@code draft} is under construction and may be incomplete or illegal;
 * {@code published} is meant to be playable and is validated before publishing.
 * The database stores the lowercase value, not the enum name.
 */
public enum DeckStatus {
	DRAFT("draft"),
	PUBLISHED("published");

	private final String value;

	DeckStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public static DeckStatus fromValue(String value) {
		return Arrays.stream(values())
			.filter(status -> status.value.equalsIgnoreCase(value))
			.findFirst()
			.orElse(DRAFT);
	}
}
