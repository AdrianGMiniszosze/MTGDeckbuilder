package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.CardTagServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardTagEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagId;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Tag Service Implementation Tests")
class CardTagServiceImplTest {

    @Mock
    private CardTagRepository cardTagRepository;

    @Mock
    private CardTagEntityMapper cardTagEntityMapper;

    @InjectMocks
    private CardTagServiceImpl cardTagService;

    @Test
    @DisplayName("Should list the tags of a card")
    void shouldFindTagsByCardId() {
        // Given
        final CardTagEntity entity = new CardTagEntity();
        final CardTag tag = CardTag.builder().cardId(1L).tagId(2L).build();
        when(this.cardTagRepository.findByCardId(1L)).thenReturn(List.of(entity));
        when(this.cardTagEntityMapper.toModelList(List.of(entity))).thenReturn(List.of(tag));

        // When
        final List<CardTag> result = this.cardTagService.findByCardId(1L);

        // Then
        assertThat(result).containsExactly(tag);
        verify(this.cardTagRepository).findByCardId(1L);
    }

    @Test
    @DisplayName("Should update an existing card tag keeping its composite key")
    void shouldUpdateExistingCardTag() {
        // Given
        final CardTag request = CardTag.builder().cardId(99L).tagId(98L).build();
        final CardTagEntity mapped = CardTagEntity.builder().build();
        final CardTagEntity saved = CardTagEntity.builder().cardId(1L).tagId(2L).build();
        final CardTag model = CardTag.builder().cardId(1L).tagId(2L).build();

        when(this.cardTagRepository.findById(new CardTagId(1L, 2L))).thenReturn(Optional.of(new CardTagEntity()));
        when(this.cardTagEntityMapper.toEntity(request)).thenReturn(mapped);
        when(this.cardTagRepository.save(mapped)).thenReturn(saved);
        when(this.cardTagEntityMapper.toModel(saved)).thenReturn(model);

        // When
        final Optional<CardTag> result = this.cardTagService.updateCardTag(1L, 2L, request);

        // Then
        assertThat(result).contains(model);
        assertThat(mapped.getCardId()).isEqualTo(1L);
        assertThat(mapped.getTagId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return empty when the card tag does not exist")
    void shouldReturnEmptyWhenCardTagMissing() {
        // Given
        when(this.cardTagRepository.findById(new CardTagId(1L, 2L))).thenReturn(Optional.empty());

        // When
        final Optional<CardTag> result = this.cardTagService.updateCardTag(1L, 2L,
                CardTag.builder().cardId(1L).tagId(2L).build());

        // Then
        assertThat(result).isEmpty();
        verify(this.cardTagRepository, never()).save(any(CardTagEntity.class));
    }

    @Test
    @DisplayName("Should delete a card tag by its composite key")
    void shouldDeleteCardTag() {
        // When
        this.cardTagService.deleteCardTag(1L, 2L);

        // Then
        final ArgumentCaptor<CardTagId> captor = ArgumentCaptor.forClass(CardTagId.class);
        verify(this.cardTagRepository).deleteById(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new CardTagId(1L, 2L));
    }
}
