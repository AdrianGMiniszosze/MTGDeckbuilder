package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardTagEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagId;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardTagServiceImpl implements CardTagService {

	private final CardTagRepository cardTagRepository;
	private final CardTagEntityMapper cardTagEntityMapper;

	@Override
	public List<CardTag> findByCardId(Long cardId) {
		// Card tags for a single card are typically a small set, so no pagination
		// needed
		return this.cardTagEntityMapper.toModelList(this.cardTagRepository.findByCardId(cardId));
	}

	@Override
	@Transactional
	public Optional<CardTag> updateCardTag(Long cardId, Long tagId, CardTag cardTag) {
		final CardTagId id = new CardTagId(cardId, tagId);
		return this.cardTagRepository.findById(id).map(existingEntity -> {
			// Update the entity with new values
			final CardTagEntity updatedEntity = this.cardTagEntityMapper.toEntity(cardTag);
			updatedEntity.setCardId(cardId);
			updatedEntity.setTagId(tagId);

			final CardTagEntity saved = this.cardTagRepository.save(updatedEntity);
			return this.cardTagEntityMapper.toModel(saved);
		});
	}

	@Override
	@Transactional
	public void deleteCardTag(Long cardId, Long tagId) {
		final CardTagId id = new CardTagId(cardId, tagId);
		this.cardTagRepository.deleteById(id);
	}
}
