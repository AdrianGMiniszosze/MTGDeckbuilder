package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardTagNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DuplicateCardTagException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidCardTagException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.TagNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardTagEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagId;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardTagServiceImpl implements CardTagService {

	private static final String DEFAULT_SOURCE = "manual";
	private static final double DEFAULT_WEIGHT = 1.0;
	private static final double DEFAULT_CONFIDENCE = 1.0;

	private final CardTagRepository cardTagRepository;
	private final CardRepository cardRepository;
	private final TagRepository tagRepository;
	private final CardTagEntityMapper cardTagEntityMapper;

	@Override
	public List<CardTag> findByCardId(Long cardId) {
		if (!this.cardRepository.existsById(cardId)) {
			throw new CardNotFoundException(cardId);
		}
		// Card tags for a single card are typically a small set, so no pagination
		// needed
		return this.cardTagEntityMapper.toModelList(this.cardTagRepository.findByCardId(cardId));
	}

	@Override
	@Transactional
	public CardTag createCardTag(Long cardId, CardTag cardTag) {
		if (!this.cardRepository.existsById(cardId)) {
			throw new CardNotFoundException(cardId);
		}
		final Long tagId = cardTag.getTagId();
		if (tagId == null) {
			throw new InvalidCardTagException("tag_id is required");
		}
		if (!this.tagRepository.existsById(tagId)) {
			throw new TagNotFoundException(tagId);
		}
		validateWeightAndConfidence(cardTag);
		if (this.cardTagRepository.existsById(new CardTagId(cardId, tagId))) {
			throw new DuplicateCardTagException(cardId, tagId);
		}

		final CardTag toCreate = CardTag.builder()
			.cardId(cardId)
			.tagId(tagId)
			.weight(cardTag.getWeight() != null ? cardTag.getWeight() : DEFAULT_WEIGHT)
			.confidence(cardTag.getConfidence() != null ? cardTag.getConfidence() : DEFAULT_CONFIDENCE)
			.source(DEFAULT_SOURCE)
			.build();

		final CardTagEntity saved = this.cardTagRepository.save(this.cardTagEntityMapper.toEntity(toCreate));
		return this.cardTagEntityMapper.toModel(saved);
	}

	@Override
	@Transactional
	public CardTag updateCardTag(Long cardId, Long tagId, CardTag cardTag) {
		final CardTagEntity entity = this.cardTagRepository.findById(new CardTagId(cardId, tagId))
			.orElseThrow(() -> new CardTagNotFoundException(cardId, tagId));

		if (cardTag.getWeight() == null && cardTag.getConfidence() == null) {
			throw new InvalidCardTagException("At least one of weight or confidence is required");
		}
		validateWeightAndConfidence(cardTag);

		if (cardTag.getWeight() != null) {
			entity.setWeight(cardTag.getWeight());
		}
		if (cardTag.getConfidence() != null) {
			entity.setConfidence(cardTag.getConfidence());
		}

		final CardTagEntity saved = this.cardTagRepository.save(entity);
		return this.cardTagEntityMapper.toModel(saved);
	}

	@Override
	@Transactional
	public void deleteCardTag(Long cardId, Long tagId) {
		final CardTagId id = new CardTagId(cardId, tagId);
		// Delete stays idempotent: the spec has no 404 response for this route.
		if (this.cardTagRepository.existsById(id)) {
			this.cardTagRepository.deleteById(id);
		}
	}

	private static void validateWeightAndConfidence(CardTag cardTag) {
		final Double weight = cardTag.getWeight();
		if (weight != null && weight < 0) {
			throw new InvalidCardTagException("weight must be zero or greater");
		}
		final Double confidence = cardTag.getConfidence();
		if (confidence != null && (confidence < 0 || confidence > 1)) {
			throw new InvalidCardTagException("confidence must be between 0 and 1");
		}
	}
}
