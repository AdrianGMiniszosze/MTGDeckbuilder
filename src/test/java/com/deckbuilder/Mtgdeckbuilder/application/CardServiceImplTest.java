package com.deckbuilder.Mtgdeckbuilder.application;

import com.deckbuilder.Mtgdeckbuilder.application.implement.CardServiceImpl;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.Mtgdeckbuilder.model.Card;
import com.deckbuilder.Mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Service Implementation Tests")
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private CardEntityMapper cardEntityMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    private Card testCard;
    private CardEntity testCardEntity;

    @BeforeEach
    void setUp() {
        testCard = Card.builder()
                .id(1L)
                .name("Lightning Bolt")
                .manaCost("{R}")
                .cmc(1)
                .colorIdentity("R")
                .typeLine("Instant")
                .cardType("Instant")
                .oracleText("Lightning Bolt deals 3 damage to any target.")
                .rarity("Common")
                .power(null)
                .toughness(null)
                .imageUrl("http://example.com/lightning-bolt.jpg")
                .foil(false)
                .unlimitedCopies(false)
                .gameChanger(false)
                .language("en")
                .build();
        
        testCardEntity = new CardEntity();
        testCardEntity.setId(1L);
        testCardEntity.setName("Lightning Bolt");
        testCardEntity.setManaCost("{R}");
        testCardEntity.setCmc(1);
        testCardEntity.setColorIdentity("R");
        testCardEntity.setTypeLine("Instant");
        testCardEntity.setCardType("Instant");
        testCardEntity.setOracleText("Lightning Bolt deals 3 damage to any target.");
        testCardEntity.setRarity("Common");
        testCardEntity.setImageUrl("http://example.com/lightning-bolt.jpg");
        testCardEntity.setFoil(false);
        testCardEntity.setUnlimitedCopies(false);
        testCardEntity.setGameChanger(false);
        testCardEntity.setLanguage("en");
    }

    @Test
    @DisplayName("Should get all cards with pagination")
    void shouldGetAllCards() {
        // Given
        CardEntity entity2 = new CardEntity();
        entity2.setId(2L);
        entity2.setName("Counterspell");
        entity2.setManaCost("{U}{U}");
        entity2.setTypeLine("Instant");
        
        Card card2 = Card.builder()
                .id(2L)
                .name("Counterspell")
                .manaCost("{U}{U}")
                .typeLine("Instant")
                .build();
        
        Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(testCardEntity, entity2));
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(entityPage);
        when(cardEntityMapper.toModel(testCardEntity)).thenReturn(testCard);
        when(cardEntityMapper.toModel(entity2)).thenReturn(card2);

        // When
        List<Card> result = cardService.getAllCards(10, 0);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
        assertThat(result.get(1).getName()).isEqualTo("Counterspell");
        verify(cardRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get card by ID when exists")
    void shouldGetCardByIdWhenExists() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCardEntity));
        when(cardEntityMapper.toModel(testCardEntity)).thenReturn(testCard);

        // When
        Optional<Card> result = cardService.getCardById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testCard);
        assertThat(result.get().getName()).isEqualTo("Lightning Bolt");
        verify(cardRepository, times(1)).findById(1L);
        verify(cardEntityMapper, times(1)).toModel(testCardEntity);
    }

    @Test
    @DisplayName("Should return empty optional when card not found")
    void shouldReturnEmptyWhenCardNotFound() {
        // Given
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Card> result = cardService.getCardById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(cardRepository, times(1)).findById(999L);
        verify(cardEntityMapper, never()).toModel(any(CardEntity.class));
    }

    @Test
    @DisplayName("Should create card")
    void shouldCreateCard() {
        // Given
        Card newCard = Card.builder()
                .name("New Card")
                .manaCost("{1}{G}")
                .typeLine("Creature")
                .build();
        CardEntity newEntity = new CardEntity();
        newEntity.setName("New Card");
        newEntity.setManaCost("{1}{G}");
        newEntity.setTypeLine("Creature");
        
        CardEntity savedEntity = new CardEntity();
        savedEntity.setId(3L);
        savedEntity.setName("New Card");
        savedEntity.setManaCost("{1}{G}");
        savedEntity.setTypeLine("Creature");
        
        Card savedCard = Card.builder()
                .id(3L)
                .name("New Card")
                .manaCost("{1}{G}")
                .typeLine("Creature")
                .build();
        
        when(cardEntityMapper.toEntity(newCard)).thenReturn(newEntity);
        when(cardRepository.save(newEntity)).thenReturn(savedEntity);
        when(cardEntityMapper.toModel(savedEntity)).thenReturn(savedCard);

        // When
        Card result = cardService.createCard(newCard);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(3L);
        assertThat(result.getName()).isEqualTo("New Card");
        verify(cardEntityMapper, times(1)).toEntity(newCard);
        verify(cardRepository, times(1)).save(newEntity);
        verify(cardEntityMapper, times(1)).toModel(savedEntity);
    }

    @Test
    @DisplayName("Should update existing card")
    void shouldUpdateExistingCard() {
        // Given
        Card updatedCard = Card.builder()
                .name("Lightning Bolt Updated")
                .manaCost("{R}")
                .typeLine("Instant")
                .build();
        CardEntity updatedEntity = new CardEntity();
        updatedEntity.setName("Lightning Bolt Updated");
        updatedEntity.setManaCost("{R}");
        updatedEntity.setTypeLine("Instant");
        
        CardEntity savedEntity = new CardEntity();
        savedEntity.setId(1L);
        savedEntity.setName("Lightning Bolt Updated");
        savedEntity.setManaCost("{R}");
        savedEntity.setTypeLine("Instant");
        
        Card savedCard = Card.builder()
                .id(1L)
                .name("Lightning Bolt Updated")
                .manaCost("{R}")
                .typeLine("Instant")
                .build();
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCardEntity));
        when(cardEntityMapper.toEntity(updatedCard)).thenReturn(updatedEntity);
        when(cardRepository.save(any(CardEntity.class))).thenReturn(savedEntity);
        when(cardEntityMapper.toModel(savedEntity)).thenReturn(savedCard);

        // When
        Optional<Card> result = cardService.updateCard(1L, updatedCard);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Lightning Bolt Updated");
        verify(cardRepository, times(1)).findById(1L);
        verify(cardRepository, times(1)).save(any(CardEntity.class));
    }

    @Test
    @DisplayName("Should return empty when updating non-existent card")
    void shouldReturnEmptyWhenUpdatingNonExistentCard() {
        // Given
        Card updatedCard = Card.builder()
                .name("Non-existent Card")
                .build();
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Card> result = cardService.updateCard(999L, updatedCard);

        // Then
        assertThat(result).isEmpty();
        verify(cardRepository, times(1)).findById(999L);
        verify(cardRepository, never()).save(any(CardEntity.class));
    }

    @Test
    @DisplayName("Should delete card")
    void shouldDeleteCard() {
        // Given
        Long cardId = 1L;
        doNothing().when(cardRepository).deleteById(cardId);

        // When
        cardService.deleteCard(cardId);

        // Then
        verify(cardRepository, times(1)).deleteById(cardId);
    }

    @Test
    @DisplayName("Should search cards by name")
    void shouldSearchCardsByName() {
        // Given
        Page<CardEntity> entityPage = new PageImpl<>(List.of(testCardEntity));
        when(cardRepository.searchByName(eq("Lightning"), any(Pageable.class))).thenReturn(entityPage);
        when(cardEntityMapper.toModel(testCardEntity)).thenReturn(testCard);

        // When
        List<Card> result = cardService.searchCards("Lightning", 10, 0);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("Lightning");
        verify(cardRepository, times(1)).searchByName(eq("Lightning"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get cards by format")
    void shouldGetCardsByFormat() {
        // Given
        when(cardRepository.findByFormatId(1L)).thenReturn(List.of(testCardEntity));
        when(cardEntityMapper.toModel(testCardEntity)).thenReturn(testCard);

        // When
        List<Card> result = cardService.getCardsByFormat(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
        verify(cardRepository, times(1)).findByFormatId(1L);
    }



    @Test
    @DisplayName("Should throw UnsupportedOperationException for findSimilarCards")
    void shouldThrowExceptionForFindSimilarCards() {
        // Given
        Double[] vector = new Double[1536];

        // When & Then
        assertThatThrownBy(() -> cardService.findSimilarCards(vector, 10))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not implemented yet");
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for findSimilarToCard")
    void shouldThrowExceptionForFindSimilarToCard() {
        // When & Then
        assertThatThrownBy(() -> cardService.findSimilarToCard(1L, 10))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Not implemented yet");
    }


}
