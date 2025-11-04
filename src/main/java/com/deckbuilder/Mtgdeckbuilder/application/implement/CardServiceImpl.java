package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.model.Card;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.Mtgdeckbuilder.application.CardService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final CardEntityMapper cardEntityMapper;

    @Override
    public List<Card> getAllCards(int pageSize, int pageNumber) {
        Page<CardEntity> page = cardRepository.findAll(PageRequest.of(pageNumber, pageSize));
        return page.getContent().stream()
                .map(cardEntityMapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Card> getCardById(Long id) {
        return cardRepository.findById(id)
                .map(cardEntityMapper::toModel);
    }

    @Override
    public List<Card> searchCards(String query, int pageSize, int pageNumber) {
        Page<CardEntity> page = cardRepository.searchByName(query, PageRequest.of(pageNumber, pageSize));
        return page.getContent().stream()
                .map(cardEntityMapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<Card> getCardsByFormat(Long formatId) {
        List<CardEntity> entities = cardRepository.findByFormatId(formatId);
        return entities.stream()
                .map(cardEntityMapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public Card createCard(Card card) {
        CardEntity entity = cardEntityMapper.toEntity(card);
        CardEntity saved = cardRepository.save(entity);
        return cardEntityMapper.toModel(saved);
    }

    @Override
    public Optional<Card> updateCard(Long id, Card card) {
        return cardRepository.findById(id)
                .map(existingEntity -> {
                    CardEntity updatedEntity = cardEntityMapper.toEntity(card);
                    updatedEntity.setId(id); // Preserve the ID
                    CardEntity saved = cardRepository.save(updatedEntity);
                    return cardEntityMapper.toModel(saved);
                });
    }

    @Override
    public void deleteCard(Long id) {
        cardRepository.deleteById(id);
    }

    @Override
    public List<Card> findSimilarCards(Double[] vector, int maxResults) {
        // TODO: Implement vector similarity search using PostgreSQL pgvector
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Card> findSimilarToCard(Long cardId, int maxResults) {
        // TODO: Implement similar card search using the vector of the given card
        throw new UnsupportedOperationException("Not implemented yet");
    }
}