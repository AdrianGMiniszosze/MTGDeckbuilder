package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.DecksApi;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CompleteDeckDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckValidationDTO;
import com.deckbuilder.apigenerator.openapi.api.model.DeckCardRequestDTO;
import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.application.implement.DeckAccessPolicy;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckValidationMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.security.CurrentActorProvider;
import com.deckbuilder.mtgdeckbuilder.model.Actor;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class DeckController implements DecksApi {
	private final DeckService deckService;
	private final DeckMapper deckMapper;
	private final DeckValidationMapper deckValidationMapper;
	private final CurrentActorProvider currentActorProvider;
	private final DeckAccessPolicy deckAccessPolicy;

	@Override
	public ResponseEntity<List<CompleteDeckDTO>> listDecks(Integer pagesize, Integer pagenumber, Integer userId,
			Integer format, String status) {
		final var decks = this.deckService.getAll(pagesize != null ? pagesize : 20,
				pagenumber != null ? pagenumber : 0,
				userId != null ? userId.longValue() : null,
				format != null ? format.longValue() : null,
				parseStatus(status), this.currentActorProvider.currentActorOrNull());
		return ResponseEntity.ok(this.deckMapper.toCompleteDecksDTO(decks));
	}

	private static DeckStatus parseStatus(String status) {
		if (status == null) {
			return null;
		}
		return switch (status.toLowerCase(Locale.ROOT)) {
			case "draft" -> DeckStatus.DRAFT;
			case "published" -> DeckStatus.PUBLISHED;
			default -> throw new InvalidDeckCompositionException(
					"Invalid status. Must be 'draft' or 'published'");
		};
	}

	@Override
	public ResponseEntity<CompleteDeckDTO> getDeckById(Integer id) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var deck = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireRead(deck, actor);
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckMapper.toCompleteDeckDTO(deck, validation));
	}

	@Override
	public ResponseEntity<DeckDTO> createDeck(@Valid DeckDTO deckDTO) {
		final Actor actor = this.currentActorProvider.requireActor();
		final var deck = this.deckMapper.toDeck(deckDTO).toBuilder().userId(actor.userId()).build();
		final var created = this.deckService.create(deck);
		final var validation = this.deckService.validate(created.getId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(this.deckMapper.toDeckDTO(created, validation));
	}

	@Override
	public ResponseEntity<DeckDTO> updateDeck(Integer id, @Valid DeckDTO deckDTO) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var existing = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireWrite(existing, actor);

		Deck deck = this.deckMapper.toDeck(deckDTO);
		// Omitted arrays preserve the stored rows; an explicit [] clears them.
		if (!DeckPayloadFields.wasSent(DeckPayloadFields.DECK_CONTENTS)) {
			deck = deck.toBuilder().cards(null).build();
		}
		if (!DeckPayloadFields.wasSent(DeckPayloadFields.COMMANDER_CARD_IDS)) {
			deck = deck.toBuilder().commanderCardIds(null).build();
		}
		final var updated = this.deckService.update(id.longValue(), deck);
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckMapper.toDeckDTO(updated, validation));
	}

	@Override
	public ResponseEntity<DeckValidationDTO> getDeckValidation(Integer id) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var deck = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireRead(deck, actor);
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckValidationMapper.toDto(validation));
	}

	@Override
	public ResponseEntity<DeckDTO> publishDeck(Integer id) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var existing = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireWrite(existing, actor);
		final var published = this.deckService.publish(id.longValue());
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckMapper.toDeckDTO(published, validation));
	}

	@Override
	public ResponseEntity<DeckDTO> unpublishDeck(Integer id) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var existing = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireWrite(existing, actor);
		final var draft = this.deckService.unpublish(id.longValue());
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckMapper.toDeckDTO(draft, validation));
	}

	@Override
	public ResponseEntity<DeckDTO> addCardToDeck(Integer id, @Valid DeckCardRequestDTO deckCardRequestDTO) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var existing = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireWrite(existing, actor);
		final String section = deckCardRequestDTO.getSection() != null
				? deckCardRequestDTO.getSection().getValue()
				: "main";
		final var updated = this.deckService.addCard(id.longValue(),
				deckCardRequestDTO.getCard_id().longValue(), deckCardRequestDTO.getQuantity(), section);
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckMapper.toDeckDTO(updated, validation));
	}

	@Override
	public ResponseEntity<DeckDTO> removeCardFromDeck(Integer id, Integer card_id, Integer quantity, String section) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var existing = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireWrite(existing, actor);
		final var updated = this.deckService.removeCard(id.longValue(), card_id.longValue(), quantity,
				section != null ? section : "main");
		final var validation = this.deckService.validate(id.longValue());
		return ResponseEntity.ok(this.deckMapper.toDeckDTO(updated, validation));
	}

	@Override
	public ResponseEntity<Void> deleteDeck(Integer id) {
		final Actor actor = this.currentActorProvider.currentActorOrNull();
		final var existing = this.deckService.findById(id.longValue())
				.orElseThrow(() -> new DeckNotFoundException(id.longValue()));
		this.deckAccessPolicy.requireWrite(existing, actor);
		this.deckService.deleteById(id.longValue());
		return ResponseEntity.noContent().build();
	}
}
