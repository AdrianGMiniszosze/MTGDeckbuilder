package com.deckbuilder.mtgdeckbuilder.model;

/**
 * Severity of a deck validation issue.
 *
 * <p>
 * Only {@code ERROR} blocks publishing; {@code WARNING} is informational and is
 * sent to the client so the editor can colour the row without parsing messages.
 */
public enum IssueSeverity {
	ERROR("error"),
	WARNING("warning");

	private final String value;

	IssueSeverity(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
