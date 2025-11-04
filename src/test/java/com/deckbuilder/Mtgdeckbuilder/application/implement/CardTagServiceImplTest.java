package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.CardTagEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.Mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardTag Service Implementation Tests")
class CardTagServiceImplTest {

    @Mock
    private CardTagRepository cardTagRepository;

    @Mock
    private CardTagEntityMapper cardTagEntityMapper;

    @InjectMocks
    private CardTagServiceImpl cardTagService;

    private CardTag testCardTag;
    private CardTag testCardTag2;
    private CardTagEntity testCardTagEntity;
    private CardTagEntity testCardTagEntity2;

    @BeforeEach
    void setUp() {
        testCardTag = CardTag.builder()
                .cardId(1L)
                .tagId(10L)
                .weight(1.0)
                .confidence(1.0)
                .source("scryfall_import")
                .modelVersion("v1.0")
                .createdAt(LocalDateTime.now())
                .build();

        testCardTag2 = CardTag.builder()
                .cardId(1L)
                .tagId(11L)
                .weight(1.0)
                .confidence(0.95)
                .source("scryfall_import")
                .modelVersion("v1.0")
                .createdAt(LocalDateTime.now())
                .build();

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
    @DisplayName("Should find all tags for a card")
    void shouldFindAllTagsByCardId() {
        // Given
        when(cardTagRepository.findByCardId(1L))
                .thenReturn(Arrays.asList(testCardTagEntity, testCardTagEntity2));
        when(cardTagEntityMapper.toModelList(anyList()))
                .thenReturn(Arrays.asList(testCardTag, testCardTag2));

        // When
        List<CardTag> result = cardTagService.findByCardId(1L, 10, 1);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CardTag::getCardId)
                .containsOnly(1L);
        assertThat(result).extracting(CardTag::getTagId)
                .containsExactly(10L, 11L);
        verify(cardTagRepository, times(1)).findByCardId(1L);
        verify(cardTagEntityMapper, times(1)).toModelList(anyList());
    }

    @Test
    @DisplayName("Should return empty list when card has no tags")
    void shouldReturnEmptyListWhenCardHasNoTags() {
        // Given
        when(cardTagRepository.findByCardId(999L)).thenReturn(List.of());
        when(cardTagEntityMapper.toModelList(anyList())).thenReturn(List.of());

        // When
        List<CardTag> result = cardTagService.findByCardId(999L, 10, 1);

        // Then
        assertThat(result).isEmpty();
        verify(cardTagRepository, times(1)).findByCardId(999L);
        verify(cardTagEntityMapper, times(1)).toModelList(anyList());
    }

    @Test
    @DisplayName("Should ignore pagination parameters for small result sets")
    void shouldIgnorePaginationParameters() {
        // Given
        when(cardTagRepository.findByCardId(1L))
                .thenReturn(Arrays.asList(testCardTagEntity, testCardTagEntity2));
        when(cardTagEntityMapper.toModelList(anyList()))
                .thenReturn(Arrays.asList(testCardTag, testCardTag2));

        // When - Call with different pagination params
        List<CardTag> result1 = cardTagService.findByCardId(1L, 5, 1);
        List<CardTag> result2 = cardTagService.findByCardId(1L, 100, 5);

        // Then - Both should return same results (pagination ignored)
        assertThat(result1).hasSize(2);
        assertThat(result2).hasSize(2);
        verify(cardTagRepository, times(2)).findByCardId(1L);
    }

    @Test
    @DisplayName("Should return tags with different confidence levels")
    void shouldReturnTagsWithDifferentConfidenceLevels() {
        // Given
        when(cardTagRepository.findByCardId(1L))
                .thenReturn(Arrays.asList(testCardTagEntity, testCardTagEntity2));
        when(cardTagEntityMapper.toModelList(anyList()))
                .thenReturn(Arrays.asList(testCardTag, testCardTag2));

        // When
        List<CardTag> result = cardTagService.findByCardId(1L, 10, 1);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CardTag::getConfidence)
                .containsExactly(1.0, 0.95);
        assertThat(result.get(0).getConfidence()).isEqualTo(1.0);
        assertThat(result.get(1).getConfidence()).isEqualTo(0.95);
    }

    @Test
    @DisplayName("Should return tags with correct source information")
    void shouldReturnTagsWithCorrectSource() {
        // Given
        when(cardTagRepository.findByCardId(1L))
                .thenReturn(Arrays.asList(testCardTagEntity));
        when(cardTagEntityMapper.toModelList(anyList()))
                .thenReturn(Arrays.asList(testCardTag));

        // When
        List<CardTag> result = cardTagService.findByCardId(1L, 10, 1);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSource()).isEqualTo("scryfall_import");
        assertThat(result.get(0).getModelVersion()).isEqualTo("v1.0");
    }

    @Test
    @DisplayName("Should handle multiple tags for single card")
    void shouldHandleMultipleTagsForSingleCard() {
        // Given
        CardTagEntity tag3 = CardTagEntity.builder()
                .cardId(1L)
                .tagId(12L)
                .weight(1.0)
                .confidence(0.85)
                .source("ml_model")
                .modelVersion("v2.0")
                .createdAt(LocalDateTime.now())
                .build();

        CardTag modelTag3 = CardTag.builder()
                .cardId(1L)
                .tagId(12L)
                .weight(1.0)
                .confidence(0.85)
                .source("ml_model")
                .modelVersion("v2.0")
                .createdAt(LocalDateTime.now())
                .build();

        when(cardTagRepository.findByCardId(1L))
                .thenReturn(Arrays.asList(testCardTagEntity, testCardTagEntity2, tag3));
        when(cardTagEntityMapper.toModelList(anyList()))
                .thenReturn(Arrays.asList(testCardTag, testCardTag2, modelTag3));

        // When
        List<CardTag> result = cardTagService.findByCardId(1L, 10, 1);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).extracting(CardTag::getTagId)
                .containsExactly(10L, 11L, 12L);
        assertThat(result).extracting(CardTag::getSource)
                .containsExactly("scryfall_import", "scryfall_import", "ml_model");
    }

    @Test
    @DisplayName("Should verify mapper is called correctly")
    void shouldVerifyMapperIsCalledCorrectly() {
        // Given
        List<CardTagEntity> entities = Arrays.asList(testCardTagEntity);
        when(cardTagRepository.findByCardId(1L)).thenReturn(entities);
        when(cardTagEntityMapper.toModelList(entities)).thenReturn(Arrays.asList(testCardTag));

        // When
        cardTagService.findByCardId(1L, 10, 1);

        // Then
        verify(cardTagEntityMapper, times(1)).toModelList(entities);
    }
}
