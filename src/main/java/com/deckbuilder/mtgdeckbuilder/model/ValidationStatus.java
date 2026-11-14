package com.deckbuilder.mtgdeckbuilder.model;

/**
 * Summary of a deck's legality.
 *
 * <p>
 * {@code UNKNOWN} means there are no errors but the format tracks legality and
 * at least one card has no legality row, usually because the card data import
 * is incomplete. It is a warning, not a blocker.
 */
public enum ValidationStatus {
	LEGAL("legal"),
	ILLEGAL("illegal"),
	UNKNOWN("unknown");

	private final String value;

	ValidationStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
