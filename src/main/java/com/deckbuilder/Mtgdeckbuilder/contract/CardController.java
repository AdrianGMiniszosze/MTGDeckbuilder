package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.CardsApi;
import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

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
		final var cards = this.cardService.getAllCards(pagesize != null ? pagesize : 10, pagenumber != null ? pagenumber : 0);
		final var cardDtos = cards.stream().map(this.cardMapper::toDto).collect(Collectors.toList());
		return ResponseEntity.ok(cardDtos);
	}

	@Override
	public ResponseEntity<CardDTO> getCardById(Integer id) {
		return this.cardService.getCardById(id.longValue()).map(card -> ResponseEntity.ok(this.cardMapper.toDto(card)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<CardDTO> createCard(@Valid CardDTO cardDTO) {
		final Card card = this.cardMapper.toEntity(cardDTO);
		final Card createdCard = this.cardService.createCard(card);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.cardMapper.toDto(createdCard));
	}

	@Override
	public ResponseEntity<CardDTO> updateCard(Integer id, @Valid CardDTO cardDTO) {
		final Card card = this.cardMapper.toEntity(cardDTO);
		return this.cardService.updateCard(id.longValue(), card)
				.map(updatedCard -> ResponseEntity.ok(this.cardMapper.toDto(updatedCard)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Void> deleteCard(Integer id) {
        this.cardService.deleteCard(id.longValue());
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<List<CardTagDTO>> listTagsForCard(Integer id, Integer pagesize, Integer pagenumber) {
		final var cardTags = this.cardTagService.findByCardId(id.longValue(), pagesize != null ? pagesize : 10,
				pagenumber != null ? pagenumber : 0);
		return ResponseEntity.ok(this.cardTagMapper.toCardTagDTOs(cardTags));
	}

	@Override
	public ResponseEntity<CardTagDTO> updateCardTag(Integer id, Integer tagId, @Valid CardTagDTO cardTagDTO) {
		final var cardTag = this.cardTagMapper.toCardTag(cardTagDTO);
		return this.cardTagService.updateCardTag(id.longValue(), tagId.longValue(), cardTag)
				.map(updated -> ResponseEntity.ok(this.cardTagMapper.toCardTagDTO(updated)))
				.orElse(ResponseEntity.notFound().build());
	}

	@Override
	public ResponseEntity<Void> deleteCardTag(Integer id, Integer tagId) {
        this.cardTagService.deleteCardTag(id.longValue(), tagId.longValue());
		return ResponseEntity.noContent().build();
	}
}