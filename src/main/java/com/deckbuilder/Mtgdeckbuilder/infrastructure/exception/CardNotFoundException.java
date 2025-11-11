package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Exception thrown when a requested card is not found
 */
public class CardNotFoundException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

	public CardNotFoundException(Long id) {
		super("Card not found with id: " + id);
	}

	public CardNotFoundException(String name) {
		super("Card not found with name: " + name);
	}
}
