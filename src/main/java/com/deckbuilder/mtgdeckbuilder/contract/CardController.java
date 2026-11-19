package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.CardsApi;
import com.deckbuilder.apigenerator.openapi.api.model.CardDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardSearchResponseDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagRequestDTO;
import com.deckbuilder.apigenerator.openapi.api.model.CardTagUpdateDTO;
import com.deckbuilder.apigenerator.openapi.api.model.PageInfoDTO;
import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardMapper;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchResult;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
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
	public ResponseEntity<List<CardDTO>> listCards(
			@RequestParam(name = "pagesize", required = false) Integer pagesize,
			@RequestParam(name = "pagenumber", required = false) Integer pagenumber,
			@RequestParam(name = "include_obsolete", required = false) Boolean includeObsolete) {
		final var cards = this.cardService.getAllCards(pagesize != null ? pagesize : 20,
				pagenumber != null ? pagenumber : 0, Boolean.TRUE.equals(includeObsolete));
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
	public ResponseEntity<List<CardTagDTO>> listTagsForCard(Integer id) {
		final var cardTags = this.cardTagService.findByCardId(id.longValue());
		return ResponseEntity.ok(this.cardTagMapper.toCardTagDTOs(cardTags));
	}

	@Override
	public ResponseEntity<CardTagDTO> createCardTag(Integer id, @Valid CardTagRequestDTO cardTagRequestDTO) {
		final var cardTag = CardTag.builder()
			.tagId(cardTagRequestDTO.getTag_id().longValue())
			.weight(cardTagRequestDTO.getWeight() != null ? cardTagRequestDTO.getWeight().doubleValue() : null)
			.confidence(
					cardTagRequestDTO.getConfidence() != null ? cardTagRequestDTO.getConfidence().doubleValue() : null)
			.build();
		final var created = this.cardTagService.createCardTag(id.longValue(), cardTag);
		return ResponseEntity.status(HttpStatus.CREATED).body(this.cardTagMapper.toCardTagDTO(created));
	}

	@Override
	public ResponseEntity<CardTagDTO> updateCardTag(Integer id, Integer tagId,
			@Valid CardTagUpdateDTO cardTagUpdateDTO) {
		final var cardTag = CardTag.builder()
			.weight(cardTagUpdateDTO.getWeight() != null ? cardTagUpdateDTO.getWeight().doubleValue() : null)
			.confidence(
					cardTagUpdateDTO.getConfidence() != null ? cardTagUpdateDTO.getConfidence().doubleValue() : null)
			.build();
		final var updated = this.cardTagService.updateCardTag(id.longValue(), tagId.longValue(), cardTag);
		return ResponseEntity.ok(this.cardTagMapper.toCardTagDTO(updated));
	}

	@Override
	public ResponseEntity<Void> deleteCardTag(Integer id, Integer tagId) {
		this.cardTagService.deleteCardTag(id.longValue(), tagId.longValue());
		return ResponseEntity.noContent().build();
	}


	@Override
	public ResponseEntity<CardSearchResponseDTO> searchCards(
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "type", required = false) String type,
			@RequestParam(name = "rarity", required = false) String rarity,
			@RequestParam(name = "colors", required = false) String colors,
			@RequestParam(name = "cmc_min", required = false) Integer cmcMin,
			@RequestParam(name = "cmc_max", required = false) Integer cmcMax,
			@RequestParam(name = "power_min", required = false) String powerMin,
			@RequestParam(name = "power_max", required = false) String powerMax,
			@RequestParam(name = "toughness_min", required = false) String toughnessMin,
			@RequestParam(name = "toughness_max", required = false) String toughnessMax,
			@RequestParam(name = "set_id", required = false) Integer setId,
			@RequestParam(name = "format_id", required = false) Integer formatId,
			@RequestParam(name = "text_contains", required = false) String textContains,
			@RequestParam(name = "keywords", required = false) String keywords,
			@RequestParam(name = "is_foil", required = false) Boolean isFoil,
			@RequestParam(name = "is_promo", required = false) Boolean isPromo,
			@RequestParam(name = "language", required = false) String language,
			@RequestParam(name = "pagesize", required = false) Integer pagesize,
			@RequestParam(name = "pagenumber", required = false) Integer pagenumber,
			@RequestParam(name = "include_obsolete", required = false) Boolean includeObsolete,
			@RequestParam(name = "sort_by", required = false) String sortBy,
			@RequestParam(name = "sort_order", required = false) String sortOrder) {

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
			.includeObsolete(Boolean.TRUE.equals(includeObsolete))
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
    public ResponseEntity<List<CardDTO>> getRandomCards(
			@RequestParam(name = "count", required = false) Integer count,
			@RequestParam(name = "type", required = false) String type,
			@RequestParam(name = "rarity", required = false) String rarity,
			@RequestParam(name = "format_id", required = false) Integer formatId) {

		count = count != null ? count : 1;

		final List<Card> randomCards = this.cardService.getRandomCards(count, type, rarity,
			formatId != null ? formatId.longValue() : null);

		final List<CardDTO> cardDTOs = randomCards.stream()
			.map(this.cardMapper::toDto)
			.toList();

		return ResponseEntity.ok(cardDTOs);
	}

}
