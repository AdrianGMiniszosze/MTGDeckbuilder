package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * A refresh token is unknown, expired or already rotated.
 */
public class InvalidRefreshTokenException extends DomainException {

	public InvalidRefreshTokenException() {
		super("Invalid or expired refresh token");
	}
}
