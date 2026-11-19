package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.DeckService;
import com.deckbuilder.mtgdeckbuilder.application.DeckValidationService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckCommanderRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotLegalException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.DeckEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckCommanderEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckServiceImpl implements DeckService {
	private final DeckRepository deckRepository;
	private final DeckEntityMapper deckEntityMapper;
	private final CardInDeckRepository cardInDeckRepository;
	private final CardRepository cardRepository;
	private final DeckValidationService deckValidationService;
	private final DeckCommanderRepository deckCommanderRepository;

	@Override
	public List<Deck> getAll(int pageSize, int pageNumber, Long userId, Long formatId, DeckStatus status) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		if (userId == null && formatId == null && status == null) {
			return this.withCards(this.deckRepository.findAll(pageable).getContent());
		}
		final String statusValue = status != null ? status.getValue() : null;
		return this.withCards(this.deckRepository.search(userId, formatId, statusValue, pageable).getContent());
	}

	@Override
	public Optional<Deck> findById(Long id) {
		return this.deckRepository.findById(id).map(this::buildDeckWithCards);
	}

	@Override
	public List<Deck> findByUserId(Long userId, int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.withCards(this.deckRepository.findByUserId(userId, pageable).getContent());
	}

	@Override
	public List<Deck> findByFormat(Long formatId) {
		return this.withCards(this.deckRepository.findByFormatId(formatId));
	}

	@Override
	@Transactional
	public Deck create(Deck deck) {
		final LocalDateTime now = LocalDateTime.now();
		deck = deck.toBuilder().created(now).modified(now).build();

		DeckEntity entity = this.deckEntityMapper.toEntity(deck);
		// New decks are always drafts: format rules are not applied when saving.
		entity.setStatus(DeckStatus.DRAFT.getValue());
		entity.setPublishedAt(null);
		entity = this.deckRepository.save(entity);

		this.replaceDeckCards(entity.getId(), deck.getCards());
		this.replaceDeckCommanders(entity.getId(), deck.getCommanderCardIds());

		return this.buildDeckWithCards(entity);
	}

	@Override
	@Transactional
	public Deck update(Long id, Deck deck) {
		final DeckEntity existing = this.deckRepository.findById(id)
				.orElseThrow(() -> new DeckNotFoundException(id));

		final LocalDateTime now = LocalDateTime.now();
		deck = deck.toBuilder().id(id).created(existing.getCreated()).modified(now).build();
		// is_private is optional in the contract; omitting it must not wipe the
		// stored value or violate the NOT NULL column (PrePersist does not run
		// on updates).
		if (deck.getIsPrivate() == null) {
			deck = deck.toBuilder().isPrivate(existing.getIsPrivate()).build();
		}

		DeckEntity entity = existing;
		this.deckEntityMapper.updateEntity(deck, entity);
		// Editing a published deck returns it to draft (ADR-007).
		entity.setStatus(DeckStatus.DRAFT.getValue());
		entity.setPublishedAt(null);
		entity = this.deckRepository.save(entity);

		this.replaceDeckCards(id, deck.getCards());
		this.replaceDeckCommanders(id, deck.getCommanderCardIds());

		return this.buildDeckWithCards(entity);
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
	@Transactional(readOnly = true)
	public DeckValidationResult validate(Long deckId) {
		return this.deckValidationService.evaluate(deckId);
	}

	@Override
	@Transactional
	public Deck publish(Long deckId) {
		final DeckValidationResult validation = this.deckValidationService.evaluate(deckId);
		if (validation.hasErrors()) {
			throw new DeckNotLegalException(deckId, validation);
		}

		final DeckEntity entity = this.deckRepository.findById(deckId)
				.orElseThrow(() -> new DeckNotFoundException(deckId));
		entity.setStatus(DeckStatus.PUBLISHED.getValue());
		entity.setPublishedAt(LocalDateTime.now());
		return this.buildDeckWithCards(this.deckRepository.save(entity));
	}

	@Override
	@Transactional
	public Deck unpublish(Long deckId) {
		final DeckEntity entity = this.deckRepository.findById(deckId)
				.orElseThrow(() -> new DeckNotFoundException(deckId));
		entity.setStatus(DeckStatus.DRAFT.getValue());
		entity.setPublishedAt(null);
		return this.buildDeckWithCards(this.deckRepository.save(entity));
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

		if (!this.cardRepository.existsById(cardId)) {
			throw new CardNotFoundException(cardId);
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

		// No trigger anymore: decks.updated_at is maintained here.
		this.updateDeckModificationTime(deckId);

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

		// No trigger anymore: decks.updated_at is maintained here, including deletes.
		this.updateDeckModificationTime(deckId);

		// Return updated deck with cards
		return this.findById(deckId).orElseThrow(() -> new DeckNotFoundException(deckId));
	}

	/**
	 * Replaces the deck contents when the payload includes them. A null list means
	 * the caller did not send deck_contents, so existing rows are preserved.
	 */
	private void replaceDeckCards(Long deckId, List<Deck.CardInDeck> cards) {
		if (cards == null) {
			return;
		}

		this.cardInDeckRepository.deleteByDeckId(deckId);

		for (final Deck.CardInDeck card : this.normalizeCards(deckId, cards)) {
			this.cardInDeckRepository.save(CardInDeckEntity.builder()
				.cardId(card.getCardId())
				.deckId(deckId)
				.quantity(card.getQuantity())
				.section(card.getSection())
				.build());
		}
	}

	/**
	 * Replaces the commander rows when the payload includes the field. A null
	 * list means the caller did not send commander_card_ids, so the current
	 * selection is preserved (same rule as deck_contents).
	 */
	private void replaceDeckCommanders(Long deckId, List<Long> commanderCardIds) {
		if (commanderCardIds == null) {
			return;
		}
		if (commanderCardIds.stream().anyMatch(Objects::isNull)) {
			throw new InvalidDeckCompositionException("Commander card ids must not be null");
		}

		this.deckCommanderRepository.deleteByDeckId(deckId);
		// Commanders use an assigned key: flushing the delete first keeps the
		// reinsertion of a kept commander from being merged into the deleted row.
		this.deckCommanderRepository.flush();
		for (final Long cardId : new LinkedHashSet<>(commanderCardIds)) {
			this.deckCommanderRepository.save(new DeckCommanderEntity(deckId, cardId));
		}
	}

	/**
	 * Validates card entries and merges duplicates of the same card and section.
	 */
	private List<Deck.CardInDeck> normalizeCards(Long deckId, List<Deck.CardInDeck> cards) {
		final Map<String, Deck.CardInDeck> merged = new LinkedHashMap<>();
		for (final Deck.CardInDeck card : cards) {
			final String section = card.getSection() != null ? card.getSection() : "main";
			if (!this.isValidSection(section)) {
				throw new InvalidDeckCompositionException(
						"Invalid section. Must be 'main', 'sideboard', or 'maybeboard'");
			}
			if (card.getCardId() == null) {
				throw new InvalidDeckCompositionException("Card id is required in deck contents");
			}
			if (card.getQuantity() <= 0) {
				throw new InvalidDeckCompositionException("Quantity must be greater than 0");
			}

			final Deck.CardInDeck normalized = new Deck.CardInDeck(card.getId(), card.getCardId(), deckId,
					card.getQuantity(), section);
			merged.merge(card.getCardId() + ":" + section, normalized,
					(existing, duplicate) -> new Deck.CardInDeck(existing.getId(), existing.getCardId(),
							existing.getDeckId(), existing.getQuantity() + duplicate.getQuantity(),
							existing.getSection()));
		}
		return List.copyOf(merged.values());
	}

	/**
	 * Builds the returned deck from the persisted row and its current card rows.
	 */
	private Deck buildDeckWithCards(DeckEntity entity) {
		final List<Deck.CardInDeck> cards = this.cardInDeckRepository.findByDeckId(entity.getId()).stream()
			.map(this::toCardInDeck)
			.toList();
		final List<Long> commanderCardIds = this.deckCommanderRepository.findByDeckId(entity.getId())
			.stream()
			.map(DeckCommanderEntity::getCardId)
			.sorted()
			.toList();
		return this.deckEntityMapper.toModel(entity)
			.toBuilder()
			.cards(cards)
			.commanderCardIds(commanderCardIds)
			.build();
	}

	/**
	 * Builds deck models attaching their card rows with a single extra query, so
	 * list endpoints stay free of N+1.
	 */
	private List<Deck> withCards(List<DeckEntity> entities) {
		if (entities.isEmpty()) {
			return List.of();
		}

		final List<Long> deckIds = entities.stream().map(DeckEntity::getId).toList();
		final Map<Long, List<Deck.CardInDeck>> cardsByDeck = this.cardInDeckRepository.findByDeckIdIn(deckIds)
			.stream()
			.collect(Collectors.groupingBy(CardInDeckEntity::getDeckId,
					Collectors.mapping(this::toCardInDeck, Collectors.toList())));
		final Map<Long, List<Long>> commandersByDeck = this.deckCommanderRepository.findByDeckIdIn(deckIds)
			.stream()
			.collect(Collectors.groupingBy(DeckCommanderEntity::getDeckId,
					Collectors.mapping(DeckCommanderEntity::getCardId,
							Collectors.collectingAndThen(Collectors.toList(), ids -> ids.stream().sorted().toList()))));

		return entities.stream()
			.map(entity -> this.deckEntityMapper.toModel(entity).toBuilder()
				.cards(cardsByDeck.getOrDefault(entity.getId(), List.of()))
				.commanderCardIds(commandersByDeck.getOrDefault(entity.getId(), List.of()))
				.build())
			.toList();
	}

	private Deck.CardInDeck toCardInDeck(CardInDeckEntity card) {
		return new Deck.CardInDeck(card.getId(), card.getCardId(), card.getDeckId(), card.getQuantity(),
				card.getSection());
	}

	/**
	 * Validates if the section is one of the allowed values
	 */
	private boolean isValidSection(String section) {
		return section != null
				&& (section.equals("main") || section.equals("sideboard") || section.equals("maybeboard"));
	}

	/**
	 * Updates the deck modification time
	 */
	private void updateDeckModificationTime(Long deckId) {
		final DeckEntity deck = this.deckRepository.findById(deckId)
			.orElseThrow(() -> new DeckNotFoundException(deckId));
		deck.setModified(LocalDateTime.now());
		// Any card change invalidates a previous publication.
		deck.setStatus(DeckStatus.DRAFT.getValue());
		deck.setPublishedAt(null);
		this.deckRepository.save(deck);
	}
}