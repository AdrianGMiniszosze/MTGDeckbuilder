package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;

/**
 * Thrown when a deck has blocking validation issues and cannot be published.
 *
 * <p>
 * It carries the full diagnosis so the API can answer {@code 409} with the
 * issues that caused the rejection. Saving never throws this: only publishing.
 */
public class DeckNotLegalException extends DomainException {

	private final transient DeckValidationResult validation;

	public DeckNotLegalException(Long deckId, DeckValidationResult validation) {
		super(String.format("Deck %d cannot be published because it has %d blocking issue(s)", deckId,
				validation.getErrors().size()));
		this.validation = validation;
	}

	public DeckValidationResult getValidation() {
		return this.validation;
	}
}
