package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.model.CardTag;

import java.util.List;

public interface CardTagService {
	List<CardTag> findByCardId(Long cardId);

	CardTag createCardTag(Long cardId, CardTag cardTag);

	CardTag updateCardTag(Long cardId, Long tagId, CardTag cardTag);

	void deleteCardTag(Long cardId, Long tagId);
}
