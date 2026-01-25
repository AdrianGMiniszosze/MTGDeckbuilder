package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.CardsApi;
import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardSearchResponseDTO;
import com.deckbuilder.apigenerator.openapi.api.model.PageInfoDTO;
import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CardController implements CardsApi {
	private final CardService cardService;
	private final CardTagService cardTagService;
	private final CardMapper cardMapper;
	private final CardTagMapper cardTagMapper;

	@Override
	public ResponseEntity<List<CardDTO>> listCards(Integer pagesize, Integer pagenumber) {
		final var cards = this.cardService.getAllCards(pagesize != null ? pagesize : 10,
				pagenumber != null ? pagenumber : 0);
		final var cardDtos = cards.stream().map(this.cardMapper::toDto).toList();
		return ResponseEntity.ok(cardDtos);
	}

	@Override
	public ResponseEntity<CardDTO> getCardById(Integer id) {
		final Card card = this.cardService.getCardById(id.longValue())
				.orElseThrow(() -> new CardNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.cardMapper.toDto(card));
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
		final Card updatedCard = this.cardService.updateCard(id.longValue(), card)
				.orElseThrow(() -> new CardNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.cardMapper.toDto(updatedCard));
	}

	@Override
	public ResponseEntity<Void> deleteCard(Integer id) {
		this.cardService.deleteCard(id.longValue());
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<List<CardTagDTO>> listTagsForCard(Integer id) {
		final var cardTags = this.cardTagService.findByCardId(id.longValue());
		return ResponseEntity.ok(this.cardTagMapper.toCardTagDTOs(cardTags));
	}

	@Override
	public ResponseEntity<CardTagDTO> updateCardTag(Integer id, Integer tagId, @Valid CardTagDTO cardTagDTO) {
		final var cardTag = this.cardTagMapper.toCardTag(cardTagDTO);
		final var updated = this.cardTagService.updateCardTag(id.longValue(), tagId.longValue(), cardTag)
				.orElseThrow(() -> new CardNotFoundException(id.longValue()));
		return ResponseEntity.ok(this.cardTagMapper.toCardTagDTO(updated));
	}

	@Override
	public ResponseEntity<Void> deleteCardTag(Integer id, Integer tagId) {
		this.cardTagService.deleteCardTag(id.longValue(), tagId.longValue());
		return ResponseEntity.noContent().build();
	}


	@Override
	public ResponseEntity<CardSearchResponseDTO> searchCards(
			String name, String type, String rarity, String colors,
			Integer cmcMin, Integer cmcMax, String powerMin, String powerMax,
			String toughnessMin, String toughnessMax, Integer setId, Integer formatId,
			String textContains, String keywords, Boolean isFoil, Boolean isPromo,
			String language, Integer pagesize, Integer pagenumber,
			String sortBy, String sortOrder) {

		// Set default values
		pagesize = pagesize != null ? pagesize : 20;
		pagenumber = pagenumber != null ? pagenumber : 0;
		sortBy = sortBy != null ? sortBy : "name";
		sortOrder = sortOrder != null ? sortOrder : "asc";
		language = language != null ? language : "en";

		// Build search criteria object
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name(name)
			.type(type)
			.rarity(rarity)
			.colors(colors)
			.cmcMin(cmcMin)
			.cmcMax(cmcMax)
			.powerMin(powerMin)
			.powerMax(powerMax)
			.toughnessMin(toughnessMin)
			.toughnessMax(toughnessMax)
			.setId(setId != null ? setId.longValue() : null)
			.formatId(formatId != null ? formatId.longValue() : null)
			.textContains(textContains)
			.keywords(keywords)
			.isFoil(isFoil)
			.isPromo(isPromo)
			.language(language)
			.sortBy(sortBy)
			.sortOrder(sortOrder)
			.build();

		// Perform search
		final CardSearchResult result = this.cardService.searchCardsWithCriteria(criteria, pagesize, pagenumber);

		// Convert to DTOs
		final List<CardDTO> cardDTOs = result.getCards().stream()
			.map(this.cardMapper::toDto)
			.toList();

		// Create page info using builder pattern
		final PageInfoDTO pageInfo = PageInfoDTO.builder()
			.page_size(pagesize)
			.has_next_page((pagenumber + 1) * pagesize < result.getTotalCount())
			.total_pages((int) Math.ceil((double) result.getTotalCount() / pagesize))
			.current_page(pagenumber)
			.has_previous_page(pagenumber > 0)
			.build();

		// Create response
		final CardSearchResponseDTO response = CardSearchResponseDTO.builder()
			.page_info(pageInfo)
			.total_count(result.getTotalCount())
			.cards(cardDTOs)
			.build();

		return ResponseEntity.ok(response);
	}

	@Override
    public ResponseEntity<List<CardDTO>> getRandomCards(Integer count, String type, String rarity, Integer formatId) {
		count = count != null ? count : 1;

		final List<Card> randomCards = this.cardService.getRandomCards(count, type, rarity,
			formatId != null ? formatId.longValue() : null);

		final List<CardDTO> cardDTOs = randomCards.stream()
			.map(this.cardMapper::toDto)
			.toList();

		return ResponseEntity.ok(cardDTOs);
	}

}
