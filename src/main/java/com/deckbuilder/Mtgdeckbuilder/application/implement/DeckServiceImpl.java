package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.DeckEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckServiceImpl implements DeckService {
	private final DeckRepository deckRepository;
	private final DeckEntityMapper deckEntityMapper;
	private final CardInDeckRepository cardInDeckRepository;

	@Override
	public List<Deck> getAll(int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.deckEntityMapper.toModelList(this.deckRepository.findAll(pageable).getContent());
	}

	@Override
	public Optional<Deck> findById(Long id) {
		return this.deckRepository.findById(id).map(this.deckEntityMapper::toModel);
	}

	@Override
	public List<Deck> findByUserId(Long userId, int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.deckEntityMapper.toModelList(this.deckRepository.findByUserId(userId, pageable).getContent());
	}

	@Override
	public List<Deck> findByFormat(Long formatId) {
		return this.deckEntityMapper.toModelList(this.deckRepository.findByFormatId(formatId));
	}

	@Override
	@Transactional
	public Deck create(Deck deck) {
		final LocalDateTime now = LocalDateTime.now();
		deck = deck.toBuilder().created(now).modified(now).build();

		DeckEntity entity = this.deckEntityMapper.toEntity(deck);
		entity = this.deckRepository.save(entity);
		return this.deckEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public Deck update(Long id, Deck deck) {
		if (!this.deckRepository.existsById(id)) {
			throw new DeckNotFoundException(id);
		}

		final LocalDateTime now = LocalDateTime.now();
		deck = deck.toBuilder().id(id).modified(now).build();

		DeckEntity entity = this.deckEntityMapper.toEntity(deck);
		entity = this.deckRepository.save(entity);
		return this.deckEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public boolean deleteById(Long id) {
		if (!this.deckRepository.existsById(id)) {
			return false;
		}
		this.deckRepository.deleteById(id);
		return true;
	}

	@Override
	@Transactional
	public Deck addCard(Long deckId, Long cardId, int quantity, String section) {
		if (!this.deckRepository.existsById(deckId)) {
			throw new DeckNotFoundException(deckId);
		}

		if (quantity <= 0) {
			throw new InvalidDeckCompositionException("Quantity must be greater than 0");
		}

		// Validate section
		if (!this.isValidSection(section)) {
			throw new InvalidDeckCompositionException("Invalid section. Must be 'main', 'sideboard', or 'maybeboard'");
		}

		// Check if card already exists in this section
		final Optional<CardInDeckEntity> existingCard = this.cardInDeckRepository
				.findByDeckIdAndCardIdAndSection(deckId, cardId, section);

		if (existingCard.isPresent()) {
			// Add to existing quantity
			final CardInDeckEntity cardInDeck = existingCard.get();
			cardInDeck.setQuantity(cardInDeck.getQuantity() + quantity);
			this.cardInDeckRepository.save(cardInDeck);
		} else {
			// Create new entry
			final CardInDeckEntity newCard = CardInDeckEntity.builder().cardId(cardId).deckId(deckId).quantity(quantity)
					.section(section).build();
			this.cardInDeckRepository.save(newCard);
		}

		// Return updated deck with cards
		return this.findById(deckId).orElseThrow(() -> new DeckNotFoundException(deckId));
	}

	@Override
	@Transactional
	public Deck removeCard(Long deckId, Long cardId, int quantity, String section) {
		if (!this.deckRepository.existsById(deckId)) {
			throw new DeckNotFoundException(deckId);
		}

		if (quantity <= 0) {
			throw new InvalidDeckCompositionException("Quantity must be greater than 0");
		}

		// Validate section
		if (!this.isValidSection(section)) {
			throw new InvalidDeckCompositionException("Invalid section. Must be 'main', 'sideboard', or 'maybeboard'");
		}

		// Check if card exists in this section
		final Optional<CardInDeckEntity> existingCard = this.cardInDeckRepository
				.findByDeckIdAndCardIdAndSection(deckId, cardId, section);

		if (existingCard.isEmpty()) {
			throw new InvalidDeckCompositionException("Card not found in deck section");
		}

		final CardInDeckEntity cardInDeck = existingCard.get();
		final int newQuantity = cardInDeck.getQuantity() - quantity;

		if (newQuantity <= 0) {
			// Delete the card if quantity reaches 0 or below
			this.cardInDeckRepository.deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		} else {
			// Update the quantity
			cardInDeck.setQuantity(newQuantity);
			this.cardInDeckRepository.save(cardInDeck);
		}

		// Note: deck modification time is automatically updated by the database trigger

		// Return updated deck with cards
		return this.findById(deckId).orElseThrow(() -> new DeckNotFoundException(deckId));
	}

	/**
	 * Validates if the section is one of the allowed values
	 */
	private boolean isValidSection(String section) {
		return section != null
				&& (section.equals("main") || section.equals("sideboard") || section.equals("maybeboard"));
	}
}