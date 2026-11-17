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
	TOO_MANY_COMMANDERS,
	INVALID_COMMANDER,
	INVALID_COMMANDER_PAIR,
	COMMANDER_NOT_LEGAL,
	COLOR_IDENTITY_VIOLATION,
	COMPANION_RULE_VIOLATION,
	LEGALITY_UNKNOWN
}
