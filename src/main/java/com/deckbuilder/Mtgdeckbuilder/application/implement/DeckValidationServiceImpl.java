package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.application.DeckValidationService;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardLegalityRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeckValidationServiceImpl implements DeckValidationService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final FormatRepository formatRepository;
    private final CardLegalityRepository cardLegalityRepository;
    private final CardInDeckRepository cardInDeckRepository;

    private static final int SIDEBOARD_MAX_SIZE = 15;
    private static final int DEFAULT_MAX_CARD_QUANTITY = 4;
    private static final int UNLIMITED_QUANTITY = 9999;

    @Override
    public void validateCardAddition(Long deckId, Long cardId, Integer quantity, String section, boolean isUpdate) {
        log.debug("Validating card addition: deckId={}, cardId={}, quantity={}, section={}, isUpdate={}",
                 deckId, cardId, quantity, section, isUpdate);

        // Skip validation for maybeboard cards
        if ("maybeboard".equals(section)) {
            return;
        }

        if (quantity == null || quantity <= 0) {
            throw new InvalidDeckCompositionException("Quantity must be greater than 0");
        }

        // Get deck and its format
        DeckEntity deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new InvalidDeckCompositionException("Deck not found with id: " + deckId));

        if (deck.getFormatId() == null) {
            throw new InvalidDeckCompositionException("Deck format is not specified");
        }

        // Get card
        CardEntity card = cardRepository.findById(cardId)
            .orElseThrow(() -> new InvalidDeckCompositionException("Card not found with id: " + cardId));

        // Get card legality once and reuse it
        Optional<CardLegalityEntity> cardLegality = cardLegalityRepository.findByCardIdAndFormatId(cardId, deck.getFormatId());

        // Check card legality
        validateCardLegality(cardLegality, deck.getFormatId());

        // Check individual card quantity limits
        int maxAllowedQuantity = getMaxAllowedQuantity(card, cardLegality, deck.getFormatId());
        if (quantity > maxAllowedQuantity) {
            throw new InvalidDeckCompositionException(
                String.format("Card quantity limit exceeded. Max allowed is %d, attempted to add %d",
                             maxAllowedQuantity, quantity));
        }

        // Check deck size limits
        validateDeckSizeLimit(deckId, quantity, section, deck.getFormatId(), isUpdate ? cardId : null);
    }

    @Override
    public void validateCardRemoval(Long deckId, Long cardId, Integer quantity, String section) {
        log.debug("Validating card removal: deckId={}, cardId={}, quantity={}, section={}",
                 deckId, cardId, quantity, section);

        if (quantity == null || quantity <= 0) {
            throw new InvalidDeckCompositionException("Quantity must be greater than 0");
        }

        // Check if card exists in this section
        Optional<CardInDeckEntity> existingCard = cardInDeckRepository
            .findByDeckIdAndCardIdAndSection(deckId, cardId, section);

        if (existingCard.isEmpty()) {
            throw new InvalidDeckCompositionException("Card not found in deck section");
        }

        int currentQuantity = existingCard.get().getQuantity();
        if (quantity > currentQuantity) {
            throw new InvalidDeckCompositionException(
                String.format("Cannot remove %d cards, only %d available", quantity, currentQuantity));
        }
    }

    @Override
    public int getMaxAllowedQuantity(Long cardId, Long formatId) {
        // Get card details and legality
        CardEntity card = cardRepository.findById(cardId)
            .orElseThrow(() -> new InvalidDeckCompositionException("Card not found with id: " + cardId));

        Optional<CardLegalityEntity> cardLegality = cardLegalityRepository.findByCardIdAndFormatId(cardId, formatId);

        return getMaxAllowedQuantity(card, cardLegality, formatId);
    }

    /**
     * Internal method that calculates max allowed quantity using pre-fetched entities
     * to avoid redundant database calls when card and legality are already known.
     */
    private int getMaxAllowedQuantity(CardEntity card, Optional<CardLegalityEntity> cardLegality, Long formatId) {
        // Check if card has unlimited copies flag
        if (Boolean.TRUE.equals(card.getUnlimitedCopies())) {
            return UNLIMITED_QUANTITY;
        }

        // Check if it's a basic land
        if ("Land".equals(card.getCardType()) && "Basic".equals(card.getCardSupertype())) {
            return UNLIMITED_QUANTITY;
        }

        // Check if card is restricted in this format
        if (cardLegality.isPresent() && "restricted".equals(cardLegality.get().getLegalityStatus())) {
            return 1;
        }

        // Check format-specific rules
        FormatEntity format = formatRepository.findById(formatId)
            .orElseThrow(() -> new InvalidDeckCompositionException("Format not found with id: " + formatId));

        if ("Commander".equals(format.getName())) {
            return 1;
        }

        // Default maximum
        return DEFAULT_MAX_CARD_QUANTITY;
    }

    @Override
    public int getMaxDeckSize(Long formatId, String section) {
        if ("sideboard".equals(section)) {
            return SIDEBOARD_MAX_SIZE;
        }

        // For main deck, use format's deck size limit
        FormatEntity format = formatRepository.findById(formatId)
            .orElseThrow(() -> new InvalidDeckCompositionException("Format not found with id: " + formatId));

        return format.getMaxDeckSize();
    }

    private void validateCardLegality(Optional<CardLegalityEntity> cardLegality, Long formatId) {
        if (cardLegality.isPresent()) {
            String legalityStatus = cardLegality.get().getLegalityStatus();
            if ("banned".equals(legalityStatus)) {
                FormatEntity format = formatRepository.findById(formatId)
                    .orElseThrow(() -> new InvalidDeckCompositionException("Format not found"));
                throw new InvalidDeckCompositionException(
                    String.format("Card is banned in the %s format", format.getName()));
            }
        }
        // If no legality record exists, assume the card is legal
    }

    private void validateDeckSizeLimit(Long deckId, Integer quantity, String section, Long formatId, Long excludeCardId) {
        int maxDeckSize = getMaxDeckSize(formatId, section);

        Integer currentTotalCards;
        if (excludeCardId != null) {
            // This is an update operation, exclude the existing quantity of this card
            currentTotalCards = cardInDeckRepository.sumQuantityByDeckIdAndSectionExcludingCard(deckId, section, excludeCardId);
        } else {
            // This is a new addition
            currentTotalCards = cardInDeckRepository.sumQuantityByDeckIdAndSection(deckId, section);
        }

        int totalAfterAddition = (currentTotalCards != null ? currentTotalCards : 0) + quantity;

        if (totalAfterAddition > maxDeckSize) {
            throw new InvalidDeckCompositionException(
                String.format("Deck section \"%s\" size limit exceeded. Max allowed is %d, total after addition would be %d",
                             section, maxDeckSize, totalAfterAddition));
        }
    }
}
