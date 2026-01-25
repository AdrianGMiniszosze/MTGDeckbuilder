package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.config.PaginationConfig;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {
	private final CardRepository cardRepository;
	private final CardEntityMapper cardEntityMapper;
	private final PaginationConfig paginationConfig;

	private PageRequest createPageRequest(int pageSize, int pageNumber) {
		pageSize = this.paginationConfig.validatePageSize(pageSize);
		pageNumber = this.paginationConfig.validatePageNumber(pageNumber);
		return PageRequest.of(pageNumber, pageSize);
	}

	@Override
	public List<Card> getAllCards(int pageSize, int pageNumber) {
		log.debug("Fetching all cards with pageSize={}, pageNumber={}", pageSize, pageNumber);

		final PageRequest pageRequest = createPageRequest(pageSize, pageNumber);
		log.debug("Validated pagination: pageSize={}, pageNumber={}", pageRequest.getPageSize(), pageRequest.getPageNumber());

		final Page<CardEntity> page = this.cardRepository.findAll(pageRequest);
		final List<Card> cards = page.getContent().stream()
			.map(this::mapEntityToModel)
			.toList();

		log.debug("Retrieved {} cards (total available: {})", cards.size(), page.getTotalElements());
		return cards;
	}

	private Card mapEntityToModel(CardEntity entity) {
		// If the card has a parent card, return the parent instead
		if (entity.getParentCardId() != null) {
			log.debug("Card {} has parent card {}, returning parent", entity.getId(), entity.getParentCardId());
			return this.cardRepository.findById(entity.getParentCardId())
				.map(this.cardEntityMapper::toModel)
				.orElse(this.cardEntityMapper.toModel(entity));
		}
		return this.cardEntityMapper.toModel(entity);
	}

	@Override
	public Optional<Card> getCardById(Long id) {
		log.debug("Fetching card with id={}", id);
		final Optional<Card> result = this.cardRepository.findById(id).map(this::mapEntityToModel);
		log.debug("Card with id={} found: {}", id, result.isPresent());
		return result;
	}

	@Override
	public Card getCardByIdRequired(Long id) {
		log.debug("Fetching card with id={} (required)", id);
		return getCardById(id)
				.orElseThrow(() -> new RuntimeException("Card not found with id: " + id));
	}

	@Override
	public List<Card> searchCards(String query, int pageSize, int pageNumber) {
		log.debug("Searching cards with query='{}', pageSize={}, pageNumber={}", query, pageSize, pageNumber);

		final PageRequest pageRequest = createPageRequest(pageSize, pageNumber);
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name(query)
			.build();
		final Page<CardEntity> page = this.cardRepository.searchCardsWithDetailedCriteria(criteria, pageRequest);
		final List<Card> cards = page.getContent().stream()
			.map(this::mapEntityToModel)
			.toList();

		log.debug("Found {} cards matching query='{}' (total available: {})", cards.size(), query,
				page.getTotalElements());
		return cards;
	}

	@Override
	public List<Card> getCardsByFormat(Long formatId) {
		log.debug("Finding cards by format: {}", formatId);
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.formatId(formatId)
			.build();
		// Use a large page size to get all results
		final Page<CardEntity> page = this.cardRepository.searchCardsWithDetailedCriteria(criteria, PageRequest.of(0, 10000));
		return page.getContent().stream()
			.map(this::mapEntityToModel)
			.toList();
	}

	@Override
	@Transactional
	public Card createCard(Card card) {
		log.info("Creating new card: name='{}', cmc={}", card.getName(), card.getCmc());

		CardEntity entity = this.cardEntityMapper.toEntity(card);
		entity = this.cardRepository.save(entity);
		final Card createdCard = this.cardEntityMapper.toModel(entity);

		log.info("Card created successfully with id={}", createdCard.getId());
		return createdCard;
	}

	@Override
	@Transactional
	public Optional<Card> updateCard(Long id, Card card) {
		log.info("Updating card with id={}", id);

		if (!this.cardRepository.existsById(id)) {
			log.warn("Card with id={} not found for update", id);
			return Optional.empty();
		}

		CardEntity entity = this.cardEntityMapper.toEntity(card);
		entity.setId(id);
		entity = this.cardRepository.save(entity);

		log.info("Card with id={} updated successfully", id);
		return Optional.of(this.cardEntityMapper.toModel(entity));
	}

	@Override
	@Transactional
	public void deleteCard(Long id) {
		log.info("Deleting card with id={}", id);
		this.cardRepository.deleteById(id);
		log.info("Card with id={} deleted successfully", id);
	}

	@Override
	public List<Card> findByCollectorNumber(String collectorNumber) {
		log.debug("Finding cards by collector number: {}", collectorNumber);
		final List<CardEntity> entities = this.cardRepository.findByCollectorNumber(collectorNumber);
		final List<Card> cards = entities.stream()
			.map(this::mapEntityToModel)
			.toList();
		log.debug("Found {} cards with collector number: {}", cards.size(), collectorNumber);
		return cards;
	}

	@Override
	public List<Card> findByNameAndSet(String name, Long setId) {
		log.debug("Finding cards by name: {} and set: {}", name, setId);
		final List<CardEntity> entities = this.cardRepository.findByNameAndCardSet(name, setId);
		final List<Card> cards = entities.stream()
			.map(this::mapEntityToModel)
			.toList();
		log.debug("Found {} cards with name: {} in set: {}", cards.size(), name, setId);
		return cards;
	}

	@Override
	public List<Card> searchCardsByType(String cardType, int pageSize, int pageNumber) {
		log.debug("Searching cards by type: {} with pageSize={}, pageNumber={}", cardType, pageSize, pageNumber);

		final PageRequest pageRequest = createPageRequest(pageSize, pageNumber);
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.type(cardType)
			.build();
		final Page<CardEntity> page = this.cardRepository.searchCardsWithDetailedCriteria(criteria, pageRequest);
		final List<Card> cards = page.getContent().stream()
			.map(this::mapEntityToModel)
			.toList();

		log.debug("Found {} cards with type: {} (total available: {})", cards.size(), cardType, page.getTotalElements());
		return cards;
	}

	@Override
	public List<Card> searchCardsByName(String name, int pageSize, int pageNumber) {
		log.debug("Searching cards by name: {} with pageSize={}, pageNumber={}", name, pageSize, pageNumber);

		final PageRequest pageRequest = createPageRequest(pageSize, pageNumber);
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name(name)
			.build();
		final Page<CardEntity> page = this.cardRepository.searchCardsWithDetailedCriteria(criteria, pageRequest);
		final List<Card> cards = page.getContent().stream()
			.map(this::mapEntityToModel)
			.distinct() // Remove duplicates in case multiple faces point to same parent card
			.toList();

		log.debug("Found {} cards matching name: {} (total available: {})", cards.size(), name, page.getTotalElements());
		return cards;
	}

	@Override
	public List<Card> searchCardsAdvanced(String name, String cardType, String rarity, int pageSize, int pageNumber) {
		log.debug("Advanced search: name={}, type={}, rarity={}, pageSize={}, pageNumber={}",
				 name, cardType, rarity, pageSize, pageNumber);

		final PageRequest pageRequest = createPageRequest(pageSize, pageNumber);
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name(name)
			.type(cardType)
			.rarity(rarity)
			.build();
		final Page<CardEntity> page = this.cardRepository.searchCardsWithDetailedCriteria(criteria, pageRequest);

		List<Card> cards = page.getContent().stream()
			.map(this::mapEntityToModel)
			.distinct() // Remove duplicates
			.toList();

		log.debug("Advanced search found {} cards (total available: {})", cards.size(), page.getTotalElements());
		return cards;
	}

	@Override
	public CardSearchResult searchCardsWithCriteria(CardSearchCriteria criteria, int pageSize, int pageNumber) {
		log.debug("Advanced search with criteria: {}, pageSize={}, pageNumber={}", criteria, pageSize, pageNumber);

		final PageRequest pageRequest = createPageRequest(pageSize, pageNumber);
		final Page<CardEntity> page = this.cardRepository.searchCardsWithDetailedCriteria(criteria, pageRequest);
		final List<Card> cards = page.getContent().stream()
			.map(this::mapEntityToModel)
			.distinct() // Remove duplicates
			.toList();

		log.debug("Advanced criteria search found {} cards (total available: {})", cards.size(), page.getTotalElements());

		return CardSearchResult.of(cards, (int) page.getTotalElements());
	}

	@Override
	public List<Card> getRandomCards(int count, String type, String rarity, Long formatId) {
		log.debug("Getting {} random cards with type={}, rarity={}, formatId={}", count, type, rarity, formatId);

		final List<CardEntity> entities = this.cardRepository.findRandomCards(count, type, rarity, formatId);
		final List<Card> cards = entities.stream()
			.map(this::mapEntityToModel)
			.distinct() // Remove duplicates in case multiple faces point to same parent card
			.toList();

		log.debug("Found {} random cards matching criteria", cards.size());
		return cards;
	}

}
