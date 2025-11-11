package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;
import java.io.Serial;
/**
 * Exception thrown when a requested tag is not found
 */
public class TagNotFoundException extends DomainException {
    @Serial
    private static final long serialVersionUID = 1L;
    public TagNotFoundException(Long id) {
        super("Tag not found with id: " + id);
    }
    public TagNotFoundException(String name) {
        super("Tag not found with name: " + name);
    }
    public TagNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
