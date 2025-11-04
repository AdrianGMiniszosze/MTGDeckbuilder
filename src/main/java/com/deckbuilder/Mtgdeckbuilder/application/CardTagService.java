package com.deckbuilder.Mtgdeckbuilder.application;

import com.deckbuilder.Mtgdeckbuilder.model.CardTag;

import java.util.List;
import java.util.Optional;

public interface CardTagService {
    List<CardTag> findByCardId(Long cardId, int pageSize, int pageNumber);
    Optional<CardTag> updateCardTag(Long cardId, Long tagId, CardTag cardTag);
    void deleteCardTag(Long cardId, Long tagId);
}
