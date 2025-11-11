package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;
import java.io.Serial;
/**
 * Exception thrown when a requested set is not found
 */
public class SetNotFoundException extends DomainException {
    @Serial
    private static final long serialVersionUID = 1L;
    public SetNotFoundException(Long id) {
        super("Set not found with id: " + id);
    }
    public SetNotFoundException(String name) {
        super("Set not found with name: " + name);
    }
    public SetNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
