package com.deckbuilder.mtgdeckbuilder.model;

/**
 * Stable codes for deck validation issues.
 *
 * <p>
 * The code is part of the contract: clients branch on it, never on the message.
 * See {@code docs/DECK_VALIDATION_MODEL.md} for the meaning of each one.
 */
public enum DeckIssueCode {
	CARD_BANNED,
	CARD_NOT_LEGAL,
	MAX_COPIES_EXCEEDED,
	SINGLETON_VIOLATION,
	SIDEBOARD_TOO_LARGE,
	DECK_TOO_LARGE,
	DECK_TOO_SMALL,
	MISSING_COMMANDER,
	COMPANION_RULE_VIOLATION,
	LEGALITY_UNKNOWN
}
