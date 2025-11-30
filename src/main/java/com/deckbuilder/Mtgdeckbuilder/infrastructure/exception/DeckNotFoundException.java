package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import java.io.Serial;

/**
 * Exception thrown when a requested deck is not found
 */
public class DeckNotFoundException extends DomainException {

    @Serial
    private static final long serialVersionUID = 1L;

	public DeckNotFoundException(Long id) {
		super("Deck not found with id: " + id);
	}

	public DeckNotFoundException(String name, Long userId) {
		super("Deck not found with name: " + name + " for user: " + userId);
	}
}
