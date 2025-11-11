package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.FormatService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.FormatEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.Format;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FormatServiceImpl implements FormatService {
	private final FormatRepository formatRepository;
	private final FormatEntityMapper formatEntityMapper;
	private final CardRepository cardRepository;
	private final CardInDeckRepository cardInDeckRepository;
	private final CardEntityMapper cardEntityMapper;

	public FormatServiceImpl(FormatRepository formatRepository, FormatEntityMapper formatEntityMapper,
			CardRepository cardRepository, CardInDeckRepository cardInDeckRepository,
			CardEntityMapper cardEntityMapper) {
		this.formatRepository = formatRepository;
		this.formatEntityMapper = formatEntityMapper;
		this.cardRepository = cardRepository;
		this.cardInDeckRepository = cardInDeckRepository;
		this.cardEntityMapper = cardEntityMapper;
	}

	@Override
	public List<Format> getAll() {
		return this.formatEntityMapper.toModelList(this.formatRepository.findAll());
	}

	public List<Format> findAll(int pageSize, int pageNumber) {
		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		return this.formatEntityMapper.toModelList(this.formatRepository.findAll(pageable).getContent());
	}

	@Override
	public Optional<Format> findById(Long id) {
		return this.formatRepository.findById(id).map(this.formatEntityMapper::toModel);
	}

	@Override
	public Optional<Format> findByName(String name) {
		return this.formatRepository.findByName(name).map(this.formatEntityMapper::toModel);
	}

	@Override
	public Format create(Format format) {
		FormatEntity entity = this.formatEntityMapper.toEntity(format);
		entity = this.formatRepository.save(entity);
		return this.formatEntityMapper.toModel(entity);
	}

	@Override
	public Format update(Long id, Format format) {
		final Optional<FormatEntity> existingFormat = this.formatRepository.findById(id);
		if (existingFormat.isEmpty()) {
			throw new IllegalArgumentException("Format not found with id: " + id);
		}
		FormatEntity entity = this.formatEntityMapper.toEntity(format);
		entity.setId(id);
		entity = this.formatRepository.save(entity);
		return this.formatEntityMapper.toModel(entity);
	}

	@Override
	public boolean deleteById(Long id) {
		if (!this.formatRepository.existsById(id)) {
			return false;
		}
        this.formatRepository.deleteById(id);
		return true;
	}

	public List<Card> findCardsByFormatId(Long formatId, int pageSize, int pageNumber) {
		final Optional<FormatEntity> format = this.formatRepository.findById(formatId);
		if (format.isEmpty()) {
			throw new IllegalArgumentException("Format not found with id: " + formatId);
		}

		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		final Page<CardEntity> cardEntities = this.cardRepository.findByFormatId(formatId, pageable);

		return this.cardEntityMapper.toModelList(cardEntities.getContent());
	}

	@Override
	public boolean isCardLegal(Long cardId, Long formatId) {
		final Optional<FormatEntity> format = this.formatRepository.findById(formatId);
		if (format.isEmpty()) {
			return false;
		}

		// Check if card is in the banned list
		return format.get().getBannedCards().stream().noneMatch(bannedCard -> bannedCard.equals(cardId.toString()));
	}

	@Override
	public boolean isDeckLegal(Long deckId, Long formatId) {
		if (!this.formatRepository.existsById(formatId)) {
			return false;
		}

		final List<CardInDeckEntity> deckCards = this.cardInDeckRepository.findByDeckId(deckId);
		if (deckCards.isEmpty()) {
			return false;
		}

		final FormatEntity formatEntity = this.formatRepository.findById(formatId).get();

		// Count cards by section
		final int mainDeckSize = this.calculateSectionSize(deckCards, "main");
		final int sideboardSize = this.calculateSectionSize(deckCards, "sideboard");

		// Validate deck size constraints
		if (mainDeckSize < formatEntity.getMinDeckSize() || mainDeckSize > formatEntity.getMaxDeckSize()) {
			return false;
		}

		if (sideboardSize > formatEntity.getMaxSideboardSize()) {
			return false;
		}

		// Check for banned cards
		return !this.containsBannedCards(deckCards, formatEntity.getBannedCards());
	}

	private int calculateSectionSize(List<CardInDeckEntity> deckCards, String section) {
		return deckCards.stream().filter(card -> section.equals(card.getSection()))
				.mapToInt(CardInDeckEntity::getQuantity).sum();
	}

	private boolean containsBannedCards(List<CardInDeckEntity> deckCards, List<String> bannedCards) {
		final Set<Long> bannedCardIds = bannedCards.stream().map(Long::parseLong).collect(Collectors.toSet());

		return deckCards.stream().filter(card -> !"maybeboard".equals(card.getSection()))
				.anyMatch(card -> bannedCardIds.contains(card.getCardId()));
	}
}