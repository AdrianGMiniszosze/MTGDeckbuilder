package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.ForbiddenOperationException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.UnauthenticatedException;
import com.deckbuilder.mtgdeckbuilder.model.Actor;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Deck Access Policy Tests")
class DeckAccessPolicyTest {

	private static final Actor OWNER = new Actor(1L, "owner", false);
	private static final Actor OTHER = new Actor(2L, "other", false);
	private static final Actor ADMIN = new Actor(9L, "admin", true);

	private final DeckAccessPolicy policy = new DeckAccessPolicy();

	@Test
	@DisplayName("Should treat a published non-private deck as public")
	void shouldDetectPublicDeck() {
		assertThat(this.policy.isPublic(deck(DeckStatus.PUBLISHED, false))).isTrue();
		assertThat(this.policy.isPublic(deck(DeckStatus.PUBLISHED, true))).isFalse();
		assertThat(this.policy.isPublic(deck(DeckStatus.DRAFT, false))).isFalse();
		assertThat(this.policy.isPublic(deck(DeckStatus.DRAFT, null))).isFalse();
	}

	@Test
	@DisplayName("Should allow reading a public deck anonymously")
	void shouldAllowAnonymousReadOfPublicDeck() {
		assertThatCode(() -> this.policy.requireRead(deck(DeckStatus.PUBLISHED, false), null))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should allow the owner and admins to read a private deck")
	void shouldAllowOwnerAndAdminRead() {
		final Deck draft = deck(DeckStatus.DRAFT, true);

		assertThatCode(() -> this.policy.requireRead(draft, OWNER)).doesNotThrowAnyException();
		assertThatCode(() -> this.policy.requireRead(draft, ADMIN)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should hide a private deck from other users")
	void shouldHidePrivateDeck() {
		assertThatThrownBy(() -> this.policy.requireRead(deck(DeckStatus.DRAFT, true), OTHER))
			.isInstanceOf(DeckNotFoundException.class);
		assertThatThrownBy(() -> this.policy.requireRead(deck(DeckStatus.DRAFT, true), null))
			.isInstanceOf(DeckNotFoundException.class);
	}

	@Test
	@DisplayName("Should require authentication to write")
	void shouldRequireActorToWrite() {
		assertThatThrownBy(() -> this.policy.requireWrite(deck(DeckStatus.DRAFT, true), null))
			.isInstanceOf(UnauthenticatedException.class);
	}

	@Test
	@DisplayName("Should allow the owner and admins to write")
	void shouldAllowOwnerAndAdminWrite() {
		final Deck draft = deck(DeckStatus.DRAFT, true);

		assertThatCode(() -> this.policy.requireWrite(draft, OWNER)).doesNotThrowAnyException();
		assertThatCode(() -> this.policy.requireWrite(draft, ADMIN)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should hide another user's private deck on write")
	void shouldHidePrivateDeckOnWrite() {
		assertThatThrownBy(() -> this.policy.requireWrite(deck(DeckStatus.DRAFT, true), OTHER))
			.isInstanceOf(DeckNotFoundException.class);
	}

	@Test
	@DisplayName("Should answer 403 when writing another user's public deck")
	void shouldForbidWritingPublicDeck() {
		assertThatThrownBy(() -> this.policy.requireWrite(deck(DeckStatus.PUBLISHED, false), OTHER))
			.isInstanceOf(ForbiddenOperationException.class);
	}

	@Test
	@DisplayName("Should allow self access and admins on user resources")
	void shouldAllowSelfOrAdmin() {
		assertThatCode(() -> this.policy.requireSelfOrAdmin(1L, OWNER)).doesNotThrowAnyException();
		assertThatCode(() -> this.policy.requireSelfOrAdmin(99L, ADMIN)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("Should reject other users and anonymous callers on user resources")
	void shouldRejectOtherUsersOnUserResources() {
		assertThatThrownBy(() -> this.policy.requireSelfOrAdmin(1L, OTHER))
			.isInstanceOf(ForbiddenOperationException.class);
		assertThatThrownBy(() -> this.policy.requireSelfOrAdmin(1L, null))
			.isInstanceOf(UnauthenticatedException.class);
	}

	private static Deck deck(DeckStatus status, Boolean isPrivate) {
		return Deck.builder().id(1L).userId(1L).status(status).isPrivate(isPrivate).build();
	}
}
