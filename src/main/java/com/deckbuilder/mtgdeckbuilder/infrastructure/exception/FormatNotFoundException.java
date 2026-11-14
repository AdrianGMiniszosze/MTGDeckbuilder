package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * Exception thrown when a requested format is not found
 */
public class FormatNotFoundException extends DomainException {

    public FormatNotFoundException(Long id) {
        super("Format not found with id: " + id);
    }

    public FormatNotFoundException(String name) {
        super("Format not found with name: " + name);
    }

    public FormatNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
