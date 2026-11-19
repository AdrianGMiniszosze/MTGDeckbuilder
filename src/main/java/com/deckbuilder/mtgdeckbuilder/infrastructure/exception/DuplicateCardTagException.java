package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Exception thrown when a card already has the requested tag.
 */
public class DuplicateCardTagException extends DomainException {

	@Serial
	private static final long serialVersionUID = 1L;

	public DuplicateCardTagException(Long cardId, Long tagId) {
		super("Card " + cardId + " already has tag " + tagId);
	}
}
