package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.application.CardTagService;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.CardTagEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagId;
import com.deckbuilder.Mtgdeckbuilder.model.CardTag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CardTagServiceImpl implements CardTagService {
    
    private final CardTagRepository cardTagRepository;
    private final CardTagEntityMapper cardTagEntityMapper;
    
    @Override
    public List<CardTag> findByCardId(Long cardId, int pageSize, int pageNumber) {
        // Pagination parameters are ignored since card tags are a small set
        return cardTagEntityMapper.toModelList(
            cardTagRepository.findByCardId(cardId)
        );
    }
    
    @Override
    public Optional<CardTag> updateCardTag(Long cardId, Long tagId, CardTag cardTag) {
        CardTagId id = new CardTagId(cardId, tagId);
        return cardTagRepository.findById(id)
            .map(existingEntity -> {
                // Update the entity with new values
                CardTagEntity updatedEntity = cardTagEntityMapper.toEntity(cardTag);
                updatedEntity.setCardId(cardId);
                updatedEntity.setTagId(tagId);
                
                CardTagEntity saved = cardTagRepository.save(updatedEntity);
                return cardTagEntityMapper.toModel(saved);
            });
    }
    
    @Override
    public void deleteCardTag(Long cardId, Long tagId) {
        CardTagId id = new CardTagId(cardId, tagId);
        cardTagRepository.deleteById(id);
    }
}
