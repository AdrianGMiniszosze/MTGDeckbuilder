package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.DecksApi;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.DeckMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeckController implements DecksApi {
	private final DeckService deckService;
	private final DeckMapper deckMapper;

	@Override
	public ResponseEntity<List<DeckDTO>> listDecks(Integer pagesize, Integer pagenumber) {
		final var decks = this.deckService.getAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.deckMapper.toDecksDTO(decks));
	}

	@Override
	public ResponseEntity<DeckDTO> getDeckById(Integer id) {
		return this.deckService.findById(id.longValue()).map(deck -> ResponseEntity.ok(this.deckMapper.toDeckDTO(deck)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<DeckDTO> createDeck(@Valid DeckDTO deckDTO) {
		final var deck = this.deckMapper.toDeck(deckDTO);
		final var created = this.deckService.create(deck);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.deckMapper.toDeckDTO(created));
	}

	@Override
	public ResponseEntity<DeckDTO> updateDeck(Integer id, @Valid DeckDTO deckDTO) {
		final var deck = this.deckMapper.toDeck(deckDTO);
		final var updated = this.deckService.update(id.longValue(), deck);
		return ResponseEntity.ok(this.deckMapper.toDeckDTO(updated));
	}

	@Override
	public ResponseEntity<Void> deleteDeck(Integer id) {
		final var deleted = this.deckService.deleteById(id.longValue());
		return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
	}
}