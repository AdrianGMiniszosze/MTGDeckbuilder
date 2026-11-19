package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

/**
 * The caller is authenticated but is not allowed to perform the operation.
 */
public class ForbiddenOperationException extends DomainException {

	public ForbiddenOperationException(String message) {
		super(message);
	}
}
