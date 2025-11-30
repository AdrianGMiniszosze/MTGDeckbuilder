package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when a requested card is not found
 */
public class CardNotFoundException extends DomainException {

	public CardNotFoundException(Long id) {
		super("Card not found with id: " + id);
	}

	public CardNotFoundException(String name) {
		super("Card not found with name: " + name);
	}
}
