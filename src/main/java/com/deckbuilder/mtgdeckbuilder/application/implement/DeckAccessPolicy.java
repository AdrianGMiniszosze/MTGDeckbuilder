package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.ForbiddenOperationException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UnauthenticatedException;
import com.deckbuilder.mtgdeckbuilder.model.Actor;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import org.springframework.stereotype.Component;

/**
 * Ownership and visibility rules for decks. A deck is publicly readable only
 * when it is published and not private; everything else is limited to its owner
 * or an administrator. Hidden decks answer 404 so their existence does not leak.
 */
@Component
public class DeckAccessPolicy {

	public boolean isPublic(Deck deck) {
		return Boolean.FALSE.equals(deck.getIsPrivate()) && deck.getStatus() == DeckStatus.PUBLISHED;
	}

	public void requireRead(Deck deck, Actor actor) {
		if (this.isPublic(deck) || canManage(deck, actor)) {
			return;
		}
		throw new DeckNotFoundException(deck.getId());
	}

	public void requireWrite(Deck deck, Actor actor) {
		if (actor == null) {
			throw new UnauthenticatedException();
		}
		if (canManage(deck, actor)) {
			return;
		}
		if (!this.isPublic(deck)) {
			// Draft or private deck owned by someone else: do not reveal it.
			throw new DeckNotFoundException(deck.getId());
		}
		throw new ForbiddenOperationException("Only the owner or an administrator can modify this deck");
	}

	public void requireSelfOrAdmin(Long targetUserId, Actor actor) {
		if (actor == null) {
			throw new UnauthenticatedException();
		}
		if (!actor.admin() && !actor.userId().equals(targetUserId)) {
			throw new ForbiddenOperationException("You can only access your own resources");
		}
	}

	private static boolean canManage(Deck deck, Actor actor) {
		return actor != null && (actor.admin() || actor.userId().equals(deck.getUserId()));
	}
}
