package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.CardsApi;
import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.Mtgdeckbuilder.application.CardService;
import com.deckbuilder.Mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CardController implements CardsApi {
    private final CardService cardService;
    private final CardTagService cardTagService;
    private final CardMapper cardMapper;
    private final CardTagMapper cardTagMapper;

    @Override
    public ResponseEntity<List<CardDTO>> listCards(Integer pagesize, Integer pagenumber) {
        var cards = cardService.getAllCards(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
        var cardDtos = cards.stream()
                          .map(cardMapper::toDto)
                          .collect(Collectors.toList());
        return ResponseEntity.ok(cardDtos);
    }

    @Override
    public ResponseEntity<CardDTO> getCardById(Integer id) {
        return cardService.getCardById(id.longValue())
                         .map(card -> ResponseEntity.ok(cardMapper.toDto(card)))
                         .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<CardDTO> createCard(@Valid CardDTO cardDTO) {
        Card card = cardMapper.toEntity(cardDTO);
        Card createdCard = cardService.createCard(card);
        return ResponseEntity.status(HttpStatus.CREATED)
                           .body(cardMapper.toDto(createdCard));
    }

    @Override
    public ResponseEntity<CardDTO> updateCard(Integer id, @Valid CardDTO cardDTO) {
        Card card = cardMapper.toEntity(cardDTO);
        return cardService.updateCard(id.longValue(), card)
                .map(updatedCard -> ResponseEntity.ok(cardMapper.toDto(updatedCard)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteCard(Integer id) {
        cardService.deleteCard(id.longValue());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CardTagDTO>> listTagsForCard(Integer id, Integer pagesize, Integer pagenumber) {
        var cardTags = cardTagService.findByCardId(id.longValue(), pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
        return ResponseEntity.ok(cardTagMapper.toCardTagDTOs(cardTags));
    }

    @Override
    public ResponseEntity<CardTagDTO> updateCardTag(Integer id, Integer tagId, @Valid CardTagDTO cardTagDTO) {
        var cardTag = cardTagMapper.toCardTag(cardTagDTO);
        return cardTagService.updateCardTag(id.longValue(), tagId.longValue(), cardTag)
                .map(updated -> ResponseEntity.ok(cardTagMapper.toCardTagDTO(updated)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteCardTag(Integer id, Integer tagId) {
        cardTagService.deleteCardTag(id.longValue(), tagId.longValue());
        return ResponseEntity.noContent().build();
    }
}