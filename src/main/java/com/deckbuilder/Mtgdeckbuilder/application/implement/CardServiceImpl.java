package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.config.PaginationConfig;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
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

	@Override
	public List<Card> getAllCards(int pageSize, int pageNumber) {
		log.debug("Fetching all cards with pageSize={}, pageNumber={}", pageSize, pageNumber);

		// Validate and normalize pagination parameters
		pageSize = this.paginationConfig.validatePageSize(pageSize);
		pageNumber = this.paginationConfig.validatePageNumber(pageNumber);

		log.debug("Validated pagination: pageSize={}, pageNumber={}", pageSize, pageNumber);

		final Page<CardEntity> page = this.cardRepository.findAll(PageRequest.of(pageNumber, pageSize));
		final List<Card> cards = page.getContent().stream().map(this.cardEntityMapper::toModel).toList();

		log.debug("Retrieved {} cards (total available: {})", cards.size(), page.getTotalElements());
		return cards;
	}

	@Override
	public Optional<Card> getCardById(Long id) {
		log.debug("Fetching card with id={}", id);
		final Optional<Card> result = this.cardRepository.findById(id).map(this.cardEntityMapper::toModel);
		log.debug("Card with id={} found: {}", id, result.isPresent());
		return result;
	}

	@Override
	public List<Card> searchCards(String query, int pageSize, int pageNumber) {
		log.debug("Searching cards with query='{}', pageSize={}, pageNumber={}", query, pageSize, pageNumber);

		// Validate and normalize pagination parameters
		pageSize = this.paginationConfig.validatePageSize(pageSize);
		pageNumber = this.paginationConfig.validatePageNumber(pageNumber);

		final Page<CardEntity> page = this.cardRepository.searchByName(query, PageRequest.of(pageNumber, pageSize));
		final List<Card> cards = page.getContent().stream().map(this.cardEntityMapper::toModel).toList();

		log.debug("Found {} cards matching query='{}' (total available: {})", cards.size(), query,
				page.getTotalElements());
		return cards;
	}

	@Override
	public List<Card> getCardsByFormat(Long formatId) {
		final List<CardEntity> entities = this.cardRepository.findByFormatId(formatId);
		return entities.stream().map(this.cardEntityMapper::toModel).toList();
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
	public List<Card> findSimilarCards(Double[] vector, int maxResults) {
		// TODO: Implement vector similarity search using PostgreSQL pgvector
		throw new UnsupportedOperationException("Not implemented yet");
	}

	@Override
	public List<Card> findSimilarToCard(Long cardId, int maxResults) {
		// TODO: Implement similar card search using the vector of the given card
		throw new UnsupportedOperationException("Not implemented yet");
	}
}
