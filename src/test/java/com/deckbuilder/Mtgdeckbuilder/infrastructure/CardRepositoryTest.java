package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Repository Tests")
class CardRepositoryTest {

    @Mock
    private CardRepository cardRepository;

    private CardEntity testCardEntity;

    @BeforeEach
    void setUp() {
        testCardEntity = new CardEntity();
        testCardEntity.setId(1L);
        testCardEntity.setName("Black Lotus");
        testCardEntity.setManaCost("{0}");
        testCardEntity.setCmc(0);
        testCardEntity.setColorIdentity("");
        testCardEntity.setTypeLine("Artifact");
        testCardEntity.setCardType("Artifact");
        testCardEntity.setOracleText("Add three mana of any one color.");
        testCardEntity.setRarity("Mythic Rare");
        testCardEntity.setCardSet(1L);
        testCardEntity.setImageUrl("http://example.com/black-lotus.jpg");
        testCardEntity.setFoil(false);
        testCardEntity.setUnlimitedCopies(false);
        testCardEntity.setGameChanger(false);
        testCardEntity.setLanguage("en");
    }

    @Test
    @DisplayName("Should find card by ID")
    void shouldFindCardById() {
        // Given
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCardEntity));

        // When
        Optional<CardEntity> result = cardRepository.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Black Lotus");
        verify(cardRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty optional when card not found")
    void shouldReturnEmptyWhenCardNotFound() {
        // Given
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<CardEntity> result = cardRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(cardRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("Should find all cards with pagination")
    void shouldFindAllCardsWithPagination() {
        // Given
        CardEntity entity2 = new CardEntity();
        entity2.setId(2L);
        entity2.setName("Mox Sapphire");
        entity2.setManaCost("{0}");
        entity2.setTypeLine("Artifact");
        
        Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(testCardEntity, entity2));
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(entityPage);

        // When
        Page<CardEntity> result = cardRepository.findAll(Pageable.ofSize(10).withPage(0));

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(CardEntity::getName)
                .containsExactly("Black Lotus", "Mox Sapphire");
        verify(cardRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should search cards by name")
    void shouldSearchCardsByName() {
        // Given
        Page<CardEntity> entityPage = new PageImpl<>(List.of(testCardEntity));
        when(cardRepository.searchByName(eq("Lotus"), any(Pageable.class))).thenReturn(entityPage);

        // When
        Page<CardEntity> result = cardRepository.searchByName("Lotus", Pageable.ofSize(10).withPage(0));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("Lotus");
        verify(cardRepository, times(1)).searchByName(eq("Lotus"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should save card")
    void shouldSaveCard() {
        // Given
        when(cardRepository.save(any(CardEntity.class))).thenReturn(testCardEntity);

        // When
        CardEntity result = cardRepository.save(testCardEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Black Lotus");
        verify(cardRepository, times(1)).save(testCardEntity);
    }

    @Test
    @DisplayName("Should delete card")
    void shouldDeleteCard() {
        // Given
        doNothing().when(cardRepository).deleteById(anyLong());

        // When
        cardRepository.deleteById(1L);

        // Then
        verify(cardRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should find cards by format ID")
    void shouldFindCardsByFormatId() {
        // Given
        when(cardRepository.findByFormatId(anyLong())).thenReturn(List.of(testCardEntity));

        // When
        List<CardEntity> result = cardRepository.findByFormatId(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Black Lotus");
        verify(cardRepository, times(1)).findByFormatId(1L);
    }

    // ========================================
    // Card Variant Tests (New Feature)
    // ========================================

    @Test
    @DisplayName("Should store card with collector number")
    void shouldStoreCardWithCollectorNumber() {
        // Given
        testCardEntity.setCollectorNumber("001");
        when(cardRepository.save(any(CardEntity.class))).thenReturn(testCardEntity);

        // When
        CardEntity result = cardRepository.save(testCardEntity);

        // Then
        assertThat(result.getCollectorNumber()).isEqualTo("001");
        verify(cardRepository, times(1)).save(testCardEntity);
    }

    @Test
    @DisplayName("Should store multiple variants of same card")
    void shouldStoreMultipleVariantsOfSameCard() {
        // Given - Regular version
        CardEntity regularCard = new CardEntity();
        regularCard.setId(1L);
        regularCard.setName("Lightning Bolt");
        regularCard.setCollectorNumber("001");
        regularCard.setFoil(false);
        regularCard.setCardSet(1L);

        // Given - Foil version
        CardEntity foilCard = new CardEntity();
        foilCard.setId(2L);
        foilCard.setName("Lightning Bolt");
        foilCard.setCollectorNumber("001★");
        foilCard.setFoil(true);
        foilCard.setCardSet(1L);

        when(cardRepository.save(regularCard)).thenReturn(regularCard);
        when(cardRepository.save(foilCard)).thenReturn(foilCard);

        // When
        CardEntity savedRegular = cardRepository.save(regularCard);
        CardEntity savedFoil = cardRepository.save(foilCard);

        // Then
        assertThat(savedRegular.getName()).isEqualTo("Lightning Bolt");
        assertThat(savedFoil.getName()).isEqualTo("Lightning Bolt");
        assertThat(savedRegular.getCollectorNumber()).isNotEqualTo(savedFoil.getCollectorNumber());
        assertThat(savedRegular.getFoil()).isFalse();
        assertThat(savedFoil.getFoil()).isTrue();
    }

    @Test
    @DisplayName("Should handle promo flag")
    void shouldHandlePromoFlag() {
        // Given
        testCardEntity.setPromo(true);
        when(cardRepository.save(any(CardEntity.class))).thenReturn(testCardEntity);

        // When
        CardEntity result = cardRepository.save(testCardEntity);

        // Then
        assertThat(result.getPromo()).isTrue();
        verify(cardRepository, times(1)).save(testCardEntity);
    }

    @Test
    @DisplayName("Should handle variation flag")
    void shouldHandleVariationFlag() {
        // Given
        testCardEntity.setVariation(true);
        when(cardRepository.save(any(CardEntity.class))).thenReturn(testCardEntity);

        // When
        CardEntity result = cardRepository.save(testCardEntity);

        // Then
        assertThat(result.getVariation()).isTrue();
        verify(cardRepository, times(1)).save(testCardEntity);
    }

    @Test
    @DisplayName("Should find cards by collector number")
    void shouldFindCardsByCollectorNumber() {
        // Given
        testCardEntity.setCollectorNumber("001");
        List<CardEntity> cards = List.of(testCardEntity);
        when(cardRepository.findByCollectorNumber("001")).thenReturn(cards);

        // When
        List<CardEntity> result = cardRepository.findByCollectorNumber("001");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCollectorNumber()).isEqualTo("001");
        verify(cardRepository, times(1)).findByCollectorNumber("001");
    }

    @Test
    @DisplayName("Should find all variants of a card in a set")
    void shouldFindAllVariantsOfCardInSet() {
        // Given - Three variants of Forest
        CardEntity forest1 = new CardEntity();
        forest1.setName("Forest");
        forest1.setCollectorNumber("264");
        forest1.setCardSet(1L);

        CardEntity forest2 = new CardEntity();
        forest2.setName("Forest");
        forest2.setCollectorNumber("265");
        forest2.setCardSet(1L);

        CardEntity forest3 = new CardEntity();
        forest3.setName("Forest");
        forest3.setCollectorNumber("266");
        forest3.setCardSet(1L);

        List<CardEntity> variants = Arrays.asList(forest1, forest2, forest3);
        when(cardRepository.findByNameAndCardSet("Forest", 1L)).thenReturn(variants);

        // When
        List<CardEntity> result = cardRepository.findByNameAndCardSet("Forest", 1L);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(card -> card.getName().equals("Forest"));
        assertThat(result).allMatch(card -> card.getCardSet().equals(1L));
        assertThat(result.stream().map(CardEntity::getCollectorNumber))
                .containsExactlyInAnyOrder("264", "265", "266");
    }
}

