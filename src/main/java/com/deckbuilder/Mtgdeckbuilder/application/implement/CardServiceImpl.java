package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
	private final CardRepository cardRepository;
	private final CardEntityMapper cardEntityMapper;

	@Override
	public List<Card> getAllCards(int pageSize, int pageNumber) {
		final Page<CardEntity> page = this.cardRepository.findAll(PageRequest.of(pageNumber, pageSize));
		return page.getContent().stream().map(this.cardEntityMapper::toModel).collect(Collectors.toList());
	}

	@Override
	public Optional<Card> getCardById(Long id) {
		return this.cardRepository.findById(id).map(this.cardEntityMapper::toModel);
	}

	@Override
	public List<Card> searchCards(String query, int pageSize, int pageNumber) {
		final Page<CardEntity> page = this.cardRepository.searchByName(query, PageRequest.of(pageNumber, pageSize));
		return page.getContent().stream().map(this.cardEntityMapper::toModel).collect(Collectors.toList());
	}

	@Override
	public List<Card> getCardsByFormat(Long formatId) {
		final List<CardEntity> entities = this.cardRepository.findByFormatId(formatId);
		return entities.stream().map(this.cardEntityMapper::toModel).collect(Collectors.toList());
	}

	@Override
	public Card createCard(Card card) {
		final CardEntity entity = this.cardEntityMapper.toEntity(card);
		final CardEntity saved = this.cardRepository.save(entity);
		return this.cardEntityMapper.toModel(saved);
	}

	@Override
	public Optional<Card> updateCard(Long id, Card card) {
		return this.cardRepository.findById(id).map(existingEntity -> {
			final CardEntity updatedEntity = this.cardEntityMapper.toEntity(card);
			updatedEntity.setId(id); // Preserve the ID
			final CardEntity saved = this.cardRepository.save(updatedEntity);
			return this.cardEntityMapper.toModel(saved);
		});
	}

	@Override
	public void deleteCard(Long id) {
        this.cardRepository.deleteById(id);
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