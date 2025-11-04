package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.model.Deck;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.DeckEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.Mtgdeckbuilder.application.DeckService;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeckServiceImpl implements DeckService {
    private final DeckRepository deckRepository;
    private final DeckEntityMapper deckEntityMapper;
    private final CardInDeckRepository cardInDeckRepository;

    @Override
    public List<Deck> getAll(int pageSize, int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return deckEntityMapper.toModelList(deckRepository.findAll(pageable).getContent());
    }

    @Override
    public Optional<Deck> findById(Long id) {
        return deckRepository.findById(id).map(deckEntityMapper::toModel);
    }

    @Override
    public List<Deck> findByUserId(Long userId, int pageSize, int pageNumber) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return deckEntityMapper.toModelList(deckRepository.findByUserId(userId, pageable).getContent());
    }

    @Override
    public List<Deck> findByFormat(Long formatId) {
        return deckEntityMapper.toModelList(deckRepository.findByFormatId(formatId));
    }

    @Override
    public Deck create(Deck deck) {
        LocalDateTime now = LocalDateTime.now();
        deck = deck.toBuilder()
                .created(now)
                .modified(now)
                .build();
        
        DeckEntity entity = deckEntityMapper.toEntity(deck);
        entity = deckRepository.save(entity);
        return deckEntityMapper.toModel(entity);
    }

    @Override
    public Deck update(Long id, Deck deck) {
        if (!deckRepository.existsById(id)) {
            throw new IllegalArgumentException("Deck not found with id: " + id);
        }
        
        LocalDateTime now = LocalDateTime.now();
        deck = deck.toBuilder()
                .id(id)
                .modified(now)
                .build();
        
        DeckEntity entity = deckEntityMapper.toEntity(deck);
        entity = deckRepository.save(entity);
        return deckEntityMapper.toModel(entity);
    }

    @Override
    public boolean deleteById(Long id) {
        if (!deckRepository.existsById(id)) {
            return false;
        }
        deckRepository.deleteById(id);
        return true;
    }

    @Override
    @Transactional
    public Deck addCard(Long deckId, Long cardId, int quantity, String section) {
        if (!deckRepository.existsById(deckId)) {
            throw new IllegalArgumentException("Deck not found with id: " + deckId);
        }
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        // Validate section
        if (!isValidSection(section)) {
            throw new IllegalArgumentException("Invalid section. Must be 'main', 'sideboard', or 'maybeboard'");
        }
        
        // Check if card already exists in this section
        Optional<CardInDeckEntity> existingCard = cardInDeckRepository
                .findByDeckIdAndCardIdAndSection(deckId, cardId, section);
        
        if (existingCard.isPresent()) {
            // Add to existing quantity
            CardInDeckEntity cardInDeck = existingCard.get();
            cardInDeck.setQuantity(cardInDeck.getQuantity() + quantity);
            cardInDeckRepository.save(cardInDeck);
        } else {
            // Create new entry
            CardInDeckEntity newCard = CardInDeckEntity.builder()
                    .cardId(cardId)
                    .deckId(deckId)
                    .quantity(quantity)
                    .section(section)
                    .build();
            cardInDeckRepository.save(newCard);
        }
                
        // Return updated deck with cards
        return findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found with id: " + deckId));
    }

    @Override
    @Transactional
    public Deck removeCard(Long deckId, Long cardId, int quantity, String section) {
        if (!deckRepository.existsById(deckId)) {
            throw new IllegalArgumentException("Deck not found with id: " + deckId);
        }
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        // Validate section
        if (!isValidSection(section)) {
            throw new IllegalArgumentException("Invalid section. Must be 'main', 'sideboard', or 'maybeboard'");
        }
        
        // Check if card exists in this section
        Optional<CardInDeckEntity> existingCard = cardInDeckRepository
                .findByDeckIdAndCardIdAndSection(deckId, cardId, section);
        
        if (existingCard.isEmpty()) {
            throw new IllegalArgumentException("Card not found in deck section");
        }
        
        CardInDeckEntity cardInDeck = existingCard.get();
        int newQuantity = cardInDeck.getQuantity() - quantity;
        
        if (newQuantity <= 0) {
            // Delete the card if quantity reaches 0 or below
            cardInDeckRepository.deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
        } else {
            // Update the quantity
            cardInDeck.setQuantity(newQuantity);
            cardInDeckRepository.save(cardInDeck);
        }
        
        // Note: deck modification time is automatically updated by the database trigger
        
        // Return updated deck with cards
        return findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found with id: " + deckId));
    }

    /**
     * Validates if the section is one of the allowed values
     */
    private boolean isValidSection(String section) {
        return section != null && 
               (section.equals("main") || section.equals("sideboard") || section.equals("maybeboard"));
    }
}