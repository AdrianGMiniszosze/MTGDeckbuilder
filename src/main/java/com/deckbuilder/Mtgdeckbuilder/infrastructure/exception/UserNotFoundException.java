package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;
import java.io.Serial;
/**
 * Exception thrown when a requested user is not found
 */
public class UserNotFoundException extends DomainException {
    @Serial
    private static final long serialVersionUID = 1L;
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id);
    }
    public UserNotFoundException(String username) {
        super("User not found with username: " + username);
    }
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
