package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Exception thrown when a card does not have the requested tag.
 */
public class CardTagNotFoundException extends DomainException {

	@Serial
	private static final long serialVersionUID = 1L;

	public CardTagNotFoundException(Long cardId, Long tagId) {
		super("Card tag not found for card " + cardId + " and tag " + tagId);
	}
}
