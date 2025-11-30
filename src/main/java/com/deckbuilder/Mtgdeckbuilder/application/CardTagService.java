package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.CardTag;

import java.util.List;
import java.util.Optional;

public interface CardTagService {
	List<CardTag> findByCardId(Long cardId);
	Optional<CardTag> updateCardTag(Long cardId, Long tagId, CardTag cardTag);
	void deleteCardTag(Long cardId, Long tagId);
}
