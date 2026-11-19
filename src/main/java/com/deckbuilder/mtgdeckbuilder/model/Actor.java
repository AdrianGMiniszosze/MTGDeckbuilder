package com.deckbuilder.mtgdeckbuilder.model;

/**
 * Authenticated caller, derived from the access token. {@code null} is used for
 * anonymous requests on public endpoints.
 */
public record Actor(Long userId, String username, boolean admin) {

	public static Actor anonymous() {
		return null;
	}
}
