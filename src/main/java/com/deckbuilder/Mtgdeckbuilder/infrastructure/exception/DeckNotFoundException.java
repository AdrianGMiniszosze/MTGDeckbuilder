package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when a requested deck is not found
 */
public class DeckNotFoundException extends DomainException {

	public DeckNotFoundException(Long id) {
		super("Deck not found with id: " + id);
	}

	public DeckNotFoundException(String name, Long userId) {
		super("Deck not found with name: " + name + " for user: " + userId);
	}
}
