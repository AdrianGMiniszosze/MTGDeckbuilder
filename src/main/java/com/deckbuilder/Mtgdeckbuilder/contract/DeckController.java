package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.DecksApi;
import com.deckbuilder.apigenerator.openapi.api.model.DeckDTO;
import com.deckbuilder.Mtgdeckbuilder.application.DeckService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.DeckMapper;
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
        var decks = deckService.getAll(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
        return ResponseEntity.ok(deckMapper.toDecksDTO(decks));
    }

    @Override
    public ResponseEntity<DeckDTO> getDeckById(Integer id) {
        return deckService.findById(id.longValue())
                .map(deck -> ResponseEntity.ok(deckMapper.toDeckDTO(deck)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<DeckDTO> createDeck(@Valid DeckDTO deckDTO) {
        var deck = deckMapper.toDeck(deckDTO);
        var created = deckService.create(deck);
        return ResponseEntity.status(HttpStatus.CREATED).body(deckMapper.toDeckDTO(created));
    }

    @Override
    public ResponseEntity<DeckDTO> updateDeck(Integer id, @Valid DeckDTO deckDTO) {
        var deck = deckMapper.toDeck(deckDTO);
        var updated = deckService.update(id.longValue(), deck);
        return ResponseEntity.ok(deckMapper.toDeckDTO(updated));
    }

    @Override
    public ResponseEntity<Void> deleteDeck(Integer id) {
        var deleted = deckService.deleteById(id.longValue());
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}