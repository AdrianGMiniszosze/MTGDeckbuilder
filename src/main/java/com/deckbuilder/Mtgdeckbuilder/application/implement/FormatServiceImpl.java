package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.application.FormatService;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.FormatEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Card;
import com.deckbuilder.Mtgdeckbuilder.model.Format;
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

    public FormatServiceImpl(FormatRepository formatRepository, 
                           FormatEntityMapper formatEntityMapper,
                           CardRepository cardRepository,
                           CardInDeckRepository cardInDeckRepository,
                           CardEntityMapper cardEntityMapper) {
        this.formatRepository = formatRepository;
        this.formatEntityMapper = formatEntityMapper;
        this.cardRepository = cardRepository;
        this.cardInDeckRepository = cardInDeckRepository;
        this.cardEntityMapper = cardEntityMapper;
    }

    @Override
    public List<Format> getAll() {
        return formatEntityMapper.toModelList(formatRepository.findAll());
    }

    public List<Format> findAll(int pageSize, int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return formatEntityMapper.toModelList(formatRepository.findAll(pageable).getContent());
    }

    @Override
    public Optional<Format> findById(Long id) {
        return formatRepository.findById(id)
                .map(formatEntityMapper::toModel);
    }

    @Override
    public Optional<Format> findByName(String name) {
        return formatRepository.findByName(name)
                .map(formatEntityMapper::toModel);
    }

    @Override
    public Format create(Format format) {
        FormatEntity entity = formatEntityMapper.toEntity(format);
        entity = formatRepository.save(entity);
        return formatEntityMapper.toModel(entity);
    }

    @Override
    public Format update(Long id, Format format) {
        Optional<FormatEntity> existingFormat = formatRepository.findById(id);
        if (existingFormat.isEmpty()) {
            throw new IllegalArgumentException("Format not found with id: " + id);
        }
        FormatEntity entity = formatEntityMapper.toEntity(format);
        entity.setId(id);
        entity = formatRepository.save(entity);
        return formatEntityMapper.toModel(entity);
    }

    @Override
    public boolean deleteById(Long id) {
        if (!formatRepository.existsById(id)) {
            return false;
        }
        formatRepository.deleteById(id);
        return true;
    }

    public List<Card> findCardsByFormatId(Long formatId, int pageSize, int pageNumber) {
        Optional<FormatEntity> format = formatRepository.findById(formatId);
        if (format.isEmpty()) {
            throw new IllegalArgumentException("Format not found with id: " + formatId);
        }
        
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<CardEntity> cardEntities = cardRepository.findByFormatId(formatId, pageable);
        
        return cardEntityMapper.toModelList(cardEntities.getContent());
    }

    @Override
    public boolean isCardLegal(Long cardId, Long formatId) {
        Optional<FormatEntity> format = formatRepository.findById(formatId);
        if (format.isEmpty()) {
            return false;
        }
        
        // Check if card is in the banned list
        return format.get().getBannedCards().stream()
                .noneMatch(bannedCard -> bannedCard.equals(cardId.toString()));
    }

    @Override
    public boolean isDeckLegal(Long deckId, Long formatId) {
        if (!formatRepository.existsById(formatId)) {
            return false;
        }
        
        List<CardInDeckEntity> deckCards = cardInDeckRepository.findByDeckId(deckId);
        if (deckCards.isEmpty()) {
            return false;
        }
        
        FormatEntity formatEntity = formatRepository.findById(formatId).get();
        
        // Count cards by section
        int mainDeckSize = calculateSectionSize(deckCards, "main");
        int sideboardSize = calculateSectionSize(deckCards, "sideboard");
        
        // Validate deck size constraints
        if (mainDeckSize < formatEntity.getMinDeckSize() || 
            mainDeckSize > formatEntity.getMaxDeckSize()) {
            return false;
        }
        
        if (sideboardSize > formatEntity.getMaxSideboardSize()) {
            return false;
        }
        
        // Check for banned cards
        return !containsBannedCards(deckCards, formatEntity.getBannedCards());
    }

    private int calculateSectionSize(List<CardInDeckEntity> deckCards, String section) {
        return deckCards.stream()
                .filter(card -> section.equals(card.getSection()))
                .mapToInt(CardInDeckEntity::getQuantity)
                .sum();
    }

    private boolean containsBannedCards(List<CardInDeckEntity> deckCards, List<String> bannedCards) {
        Set<Long> bannedCardIds = bannedCards.stream()
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        
        return deckCards.stream()
                .filter(card -> !"maybeboard".equals(card.getSection()))
                .anyMatch(card -> bannedCardIds.contains(card.getCardId()));
    }
}