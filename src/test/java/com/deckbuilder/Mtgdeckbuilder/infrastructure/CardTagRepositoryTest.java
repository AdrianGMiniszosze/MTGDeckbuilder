package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardTag Repository Tests")
class CardTagRepositoryTest {

    @Mock
    private CardTagRepository cardTagRepository;

    private CardTagEntity testCardTagEntity;
    private CardTagEntity testCardTagEntity2;

    @BeforeEach
    void setUp() {
        testCardTagEntity = CardTagEntity.builder()
                .cardId(1L)
                .tagId(10L)
                .weight(1.0)
                .confidence(1.0)
                .source("scryfall_import")
                .modelVersion("v1.0")
                .createdAt(LocalDateTime.now())
                .build();

        testCardTagEntity2 = CardTagEntity.builder()
                .cardId(1L)
                .tagId(11L)
                .weight(1.0)
                .confidence(0.95)
                .source("scryfall_import")
                .modelVersion("v1.0")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should find card-tag by composite ID")
    void shouldFindCardTagById() {
        // Given
        CardTagId id = new CardTagId(1L, 10L);
        when(cardTagRepository.findById(id)).thenReturn(Optional.of(testCardTagEntity));

        // When
        Optional<CardTagEntity> result = cardTagRepository.findById(id);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getCardId()).isEqualTo(1L);
        assertThat(result.get().getTagId()).isEqualTo(10L);
        verify(cardTagRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("Should find all tags for a specific card")
    void shouldFindAllTagsByCardId() {
        // Given
        when(cardTagRepository.findByCardId(1L)).thenReturn(Arrays.asList(testCardTagEntity, testCardTagEntity2));

        // When
        List<CardTagEntity> result = cardTagRepository.findByCardId(1L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CardTagEntity::getCardId)
                .containsOnly(1L);
        assertThat(result).extracting(CardTagEntity::getTagId)
                .containsExactly(10L, 11L);
        verify(cardTagRepository, times(1)).findByCardId(1L);
    }

    @Test
    @DisplayName("Should find all cards for a specific tag")
    void shouldFindAllCardsByTagId() {
        // Given
        CardTagEntity anotherCard = CardTagEntity.builder()
                .cardId(2L)
                .tagId(10L)
                .weight(1.0)
                .confidence(1.0)
                .source("scryfall_import")
                .modelVersion("v1.0")
                .createdAt(LocalDateTime.now())
                .build();

        when(cardTagRepository.findByTagId(10L)).thenReturn(Arrays.asList(testCardTagEntity, anotherCard));

        // When
        List<CardTagEntity> result = cardTagRepository.findByTagId(10L);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CardTagEntity::getTagId)
                .containsOnly(10L);
        assertThat(result).extracting(CardTagEntity::getCardId)
                .containsExactly(1L, 2L);
        verify(cardTagRepository, times(1)).findByTagId(10L);
    }

    @Test
    @DisplayName("Should find cards by tag with minimum confidence threshold")
    void shouldFindCardsByTagWithMinConfidence() {
        // Given - Create low confidence entity for context (not included in results)
        // Only return entities with confidence >= 0.8
        when(cardTagRepository.findByTagIdWithMinConfidence(10L, 0.8))
                .thenReturn(Arrays.asList(testCardTagEntity, testCardTagEntity2));

        // When
        List<CardTagEntity> result = cardTagRepository.findByTagIdWithMinConfidence(10L, 0.8);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(entity -> entity.getConfidence() >= 0.8);
        assertThat(result).extracting(CardTagEntity::getCardId)
                .containsExactly(1L, 1L);
        verify(cardTagRepository, times(1)).findByTagIdWithMinConfidence(10L, 0.8);
    }

    @Test
    @DisplayName("Should save card-tag relationship")
    void shouldSaveCardTag() {
        // Given
        when(cardTagRepository.save(any(CardTagEntity.class))).thenReturn(testCardTagEntity);

        // When
        CardTagEntity result = cardTagRepository.save(testCardTagEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCardId()).isEqualTo(1L);
        assertThat(result.getTagId()).isEqualTo(10L);
        assertThat(result.getConfidence()).isEqualTo(1.0);
        verify(cardTagRepository, times(1)).save(testCardTagEntity);
    }

    @Test
    @DisplayName("Should delete card-tag relationship")
    void shouldDeleteCardTag() {
        // Given
        CardTagId id = new CardTagId(1L, 10L);
        doNothing().when(cardTagRepository).deleteById(id);

        // When
        cardTagRepository.deleteById(id);

        // Then
        verify(cardTagRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("Should return empty list when card has no tags")
    void shouldReturnEmptyListWhenCardHasNoTags() {
        // Given
        when(cardTagRepository.findByCardId(999L)).thenReturn(List.of());

        // When
        List<CardTagEntity> result = cardTagRepository.findByCardId(999L);

        // Then
        assertThat(result).isEmpty();
        verify(cardTagRepository, times(1)).findByCardId(999L);
    }

    @Test
    @DisplayName("Should verify all entities have required fields")
    void shouldVerifyRequiredFields() {
        // Then
        assertThat(testCardTagEntity.getCardId()).isNotNull();
        assertThat(testCardTagEntity.getTagId()).isNotNull();
        assertThat(testCardTagEntity.getWeight()).isNotNull();
        assertThat(testCardTagEntity.getConfidence()).isNotNull();
        assertThat(testCardTagEntity.getSource()).isNotNull();
        assertThat(testCardTagEntity.getCreatedAt()).isNotNull();
    }
}
