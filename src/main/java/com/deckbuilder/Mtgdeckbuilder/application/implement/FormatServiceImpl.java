package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.FormatService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.FormatNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.FormatEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import com.deckbuilder.mtgdeckbuilder.model.Format;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class FormatServiceImpl implements FormatService {
	private final FormatRepository formatRepository;
	private final FormatEntityMapper formatEntityMapper;
	private final CardRepository cardRepository;
	private final CardEntityMapper cardEntityMapper;

	@Override
	public List<Format> getAll() {
		return this.formatEntityMapper.toModelList(this.formatRepository.findAll());
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
	@Transactional
	public Format create(Format format) {
		FormatEntity entity = this.formatEntityMapper.toEntity(format);
		entity = this.formatRepository.save(entity);
		return this.formatEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public Format update(Long id, Format format) {
		final Optional<FormatEntity> existingFormat = this.formatRepository.findById(id);
		if (existingFormat.isEmpty()) {
			throw new FormatNotFoundException(id);
		}
		FormatEntity entity = this.formatEntityMapper.toEntity(format);
		entity.setId(id);
		entity = this.formatRepository.save(entity);
		return this.formatEntityMapper.toModel(entity);
	}

	@Override
	@Transactional
	public boolean deleteById(Long id) {
		if (!this.formatRepository.existsById(id)) {
			return false;
		}
		this.formatRepository.deleteById(id);
		return true;
	}

	@Override
	public List<Card> findCardsByFormatId(Long formatId, int pageSize, int pageNumber) {
		final Optional<FormatEntity> format = this.formatRepository.findById(formatId);
		if (format.isEmpty()) {
			throw new FormatNotFoundException(formatId);
		}

		final Pageable pageable = PageRequest.of(pageNumber, pageSize);
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.formatId(formatId)
			.build();
		final Page<CardEntity> cardEntities = this.cardRepository.searchCardsWithDetailedCriteria(criteria, pageable);

		return this.cardEntityMapper.toModelList(cardEntities.getContent());
	}

	@Override
	public boolean isCardLegal(Long cardId, Long formatId) {
		final Optional<FormatEntity> format = this.formatRepository.findById(formatId);
		return format.map(formatEntity -> formatEntity.getBannedCards().stream()
				.noneMatch(bannedCard -> bannedCard.equals(cardId.toString()))).orElse(false);
	}

	@Override
	public boolean isCardRestricted(Long cardId, Long formatId) {
		final Optional<FormatEntity> format = this.formatRepository.findById(formatId);
		if (format.isEmpty()) {
			return false;
		}

		final List<String> restrictedCards = format.get().getRestrictedCards();
		if (restrictedCards == null) {
			return false;
		}

		// Check if card ID is in the restricted list
		return restrictedCards.stream()
				.anyMatch(restrictedCard -> restrictedCard.equals(cardId.toString()));
	}

	@Override
	public List<String> getRestrictedCards(Long formatId) {
		final Optional<FormatEntity> format = this.formatRepository.findById(formatId);
		if (format.isEmpty()) {
			throw new FormatNotFoundException(formatId);
		}

		final List<String> restrictedCards = format.get().getRestrictedCards();
		return restrictedCards != null ? restrictedCards : List.of();
	}
}