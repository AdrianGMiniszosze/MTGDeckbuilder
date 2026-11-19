package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.CardTagServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardTagNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DuplicateCardTagException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidCardTagException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.TagNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
	private CardRepository cardRepository;

	@Mock
	private TagRepository tagRepository;

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
		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.cardTagRepository.findByCardId(1L)).thenReturn(List.of(entity));
		when(this.cardTagEntityMapper.toModelList(List.of(entity))).thenReturn(List.of(tag));

		// When
		final List<CardTag> result = this.cardTagService.findByCardId(1L);

		// Then
		assertThat(result).containsExactly(tag);
		verify(this.cardTagRepository).findByCardId(1L);
	}

	@Test
	@DisplayName("Should reject listing tags of a missing card")
	void shouldRejectListingTagsOfMissingCard() {
		when(this.cardRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> this.cardTagService.findByCardId(99L)).isInstanceOf(CardNotFoundException.class)
			.hasMessageContaining("Card not found with id: 99");

		verify(this.cardTagRepository, never()).findByCardId(any());
	}

	@Test
	@DisplayName("Should create a card tag with the provided scores")
	void shouldCreateCardTag() {
		// Given
		final CardTag request = CardTag.builder().tagId(2L).weight(0.5).confidence(0.75).build();
		final CardTagEntity mapped = CardTagEntity.builder().build();
		final CardTagEntity saved = CardTagEntity.builder().cardId(1L).tagId(2L).build();
		final CardTag model = CardTag.builder().cardId(1L).tagId(2L).weight(0.5).confidence(0.75).build();

		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(true);
		when(this.cardTagRepository.existsById(new CardTagId(1L, 2L))).thenReturn(false);
		when(this.cardTagEntityMapper.toEntity(any(CardTag.class))).thenReturn(mapped);
		when(this.cardTagRepository.save(mapped)).thenReturn(saved);
		when(this.cardTagEntityMapper.toModel(saved)).thenReturn(model);

		// When
		final CardTag result = this.cardTagService.createCardTag(1L, request);

		// Then
		assertThat(result).isEqualTo(model);
		final ArgumentCaptor<CardTag> captor = ArgumentCaptor.forClass(CardTag.class);
		verify(this.cardTagEntityMapper).toEntity(captor.capture());
		assertThat(captor.getValue().getCardId()).isEqualTo(1L);
		assertThat(captor.getValue().getTagId()).isEqualTo(2L);
		assertThat(captor.getValue().getWeight()).isEqualTo(0.5);
		assertThat(captor.getValue().getConfidence()).isEqualTo(0.75);
		assertThat(captor.getValue().getSource()).isEqualTo("manual");
	}

	@Test
	@DisplayName("Should create a card tag with default scores")
	void shouldCreateCardTagWithDefaults() {
		// Given
		final CardTag request = CardTag.builder().tagId(2L).build();
		final CardTagEntity mapped = CardTagEntity.builder().build();
		final CardTagEntity saved = CardTagEntity.builder().cardId(1L).tagId(2L).build();

		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(true);
		when(this.cardTagRepository.existsById(new CardTagId(1L, 2L))).thenReturn(false);
		when(this.cardTagEntityMapper.toEntity(any(CardTag.class))).thenReturn(mapped);
		when(this.cardTagRepository.save(mapped)).thenReturn(saved);
		when(this.cardTagEntityMapper.toModel(saved)).thenReturn(CardTag.builder().build());

		// When
		this.cardTagService.createCardTag(1L, request);

		// Then
		final ArgumentCaptor<CardTag> captor = ArgumentCaptor.forClass(CardTag.class);
		verify(this.cardTagEntityMapper).toEntity(captor.capture());
		assertThat(captor.getValue().getWeight()).isEqualTo(1.0);
		assertThat(captor.getValue().getConfidence()).isEqualTo(1.0);
	}

	@Test
	@DisplayName("Should reject creating a tag for a missing card")
	void shouldRejectCreateForMissingCard() {
		when(this.cardRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> this.cardTagService.createCardTag(99L, CardTag.builder().tagId(2L).build()))
			.isInstanceOf(CardNotFoundException.class);

		verify(this.tagRepository, never()).existsById(any());
	}

	@Test
	@DisplayName("Should reject creating a tag without tag id")
	void shouldRejectCreateWithoutTagId() {
		when(this.cardRepository.existsById(1L)).thenReturn(true);

		assertThatThrownBy(() -> this.cardTagService.createCardTag(1L, CardTag.builder().build()))
			.isInstanceOf(InvalidCardTagException.class).hasMessageContaining("tag_id is required");
	}

	@Test
	@DisplayName("Should reject creating a tag that does not exist")
	void shouldRejectCreateWithMissingTag() {
		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(false);

		assertThatThrownBy(() -> this.cardTagService.createCardTag(1L, CardTag.builder().tagId(2L).build()))
			.isInstanceOf(TagNotFoundException.class).hasMessageContaining("Tag not found with id: 2");
	}

	@Test
	@DisplayName("Should reject a duplicate card tag")
	void shouldRejectDuplicateCardTag() {
		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(true);
		when(this.cardTagRepository.existsById(new CardTagId(1L, 2L))).thenReturn(true);

		assertThatThrownBy(() -> this.cardTagService.createCardTag(1L, CardTag.builder().tagId(2L).build()))
			.isInstanceOf(DuplicateCardTagException.class).hasMessageContaining("already has tag 2");

		verify(this.cardTagRepository, never()).save(any(CardTagEntity.class));
	}

	@Test
	@DisplayName("Should reject a negative weight")
	void shouldRejectNegativeWeight() {
		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(true);

		assertThatThrownBy(() -> this.cardTagService.createCardTag(1L,
				CardTag.builder().tagId(2L).weight(-0.1).build()))
			.isInstanceOf(InvalidCardTagException.class).hasMessageContaining("weight must be zero or greater");
	}

	@Test
	@DisplayName("Should reject a confidence above one")
	void shouldRejectConfidenceAboveOne() {
		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(true);

		assertThatThrownBy(
				() -> this.cardTagService.createCardTag(1L, CardTag.builder().tagId(2L).confidence(1.1).build()))
			.isInstanceOf(InvalidCardTagException.class).hasMessageContaining("confidence must be between 0 and 1");
	}

	@Test
	@DisplayName("Should reject a negative confidence")
	void shouldRejectNegativeConfidence() {
		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(2L)).thenReturn(true);

		assertThatThrownBy(
				() -> this.cardTagService.createCardTag(1L, CardTag.builder().tagId(2L).confidence(-0.1).build()))
			.isInstanceOf(InvalidCardTagException.class).hasMessageContaining("confidence must be between 0 and 1");
	}

	@Test
	@DisplayName("Should update only the provided fields of an existing card tag")
	void shouldUpdateExistingCardTag() {
		// Given
		final CardTag request = CardTag.builder().weight(0.9).build();
		final CardTagEntity existing = CardTagEntity.builder().cardId(1L).tagId(2L).weight(0.1).confidence(0.4)
				.source("ai_text")
				.build();
		final CardTagEntity saved = CardTagEntity.builder().cardId(1L).tagId(2L).weight(0.9).confidence(0.4)
				.source("ai_text")
				.build();
		final CardTag model = CardTag.builder().cardId(1L).tagId(2L).build();

		when(this.cardTagRepository.findById(new CardTagId(1L, 2L))).thenReturn(Optional.of(existing));
		when(this.cardTagRepository.save(existing)).thenReturn(saved);
		when(this.cardTagEntityMapper.toModel(saved)).thenReturn(model);

		// When
		final CardTag result = this.cardTagService.updateCardTag(1L, 2L, request);

		// Then
		assertThat(result).isEqualTo(model);
		assertThat(existing.getWeight()).isEqualTo(0.9);
		assertThat(existing.getConfidence()).isEqualTo(0.4);
		assertThat(existing.getSource()).isEqualTo("ai_text");
	}

	@Test
	@DisplayName("Should update only the confidence when no weight is provided")
	void shouldUpdateConfidenceOnly() {
		final CardTagEntity existing = CardTagEntity.builder().cardId(1L).tagId(2L).weight(0.1).confidence(0.4)
				.build();
		when(this.cardTagRepository.findById(new CardTagId(1L, 2L))).thenReturn(Optional.of(existing));
		when(this.cardTagRepository.save(existing)).thenReturn(existing);
		when(this.cardTagEntityMapper.toModel(existing)).thenReturn(CardTag.builder().build());

		this.cardTagService.updateCardTag(1L, 2L, CardTag.builder().confidence(0.8).build());

		assertThat(existing.getWeight()).isEqualTo(0.1);
		assertThat(existing.getConfidence()).isEqualTo(0.8);
	}

	@Test
	@DisplayName("Should reject an update without any field")
	void shouldRejectEmptyUpdate() {
		when(this.cardTagRepository.findById(new CardTagId(1L, 2L)))
			.thenReturn(Optional.of(CardTagEntity.builder().build()));

		assertThatThrownBy(() -> this.cardTagService.updateCardTag(1L, 2L, CardTag.builder().build()))
			.isInstanceOf(InvalidCardTagException.class)
			.hasMessageContaining("At least one of weight or confidence is required");

		verify(this.cardTagRepository, never()).save(any(CardTagEntity.class));
	}

	@Test
	@DisplayName("Should reject updating a missing card tag")
	void shouldRejectUpdateWhenCardTagMissing() {
		// Given
		when(this.cardTagRepository.findById(new CardTagId(1L, 2L))).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.cardTagService.updateCardTag(1L, 2L, CardTag.builder().weight(0.5).build()))
			.isInstanceOf(CardTagNotFoundException.class);

		verify(this.cardTagRepository, never()).save(any(CardTagEntity.class));
	}

	@Test
	@DisplayName("Should validate an update with out-of-range values")
	void shouldRejectInvalidUpdateValues() {
		when(this.cardTagRepository.findById(new CardTagId(1L, 2L)))
			.thenReturn(Optional.of(CardTagEntity.builder().build()));

		assertThatThrownBy(() -> this.cardTagService.updateCardTag(1L, 2L, CardTag.builder().weight(-1.0).build()))
			.isInstanceOf(InvalidCardTagException.class);
	}

	@Test
	@DisplayName("Should delete a card tag by its composite key")
	void shouldDeleteCardTag() {
		// Given
		when(this.cardTagRepository.existsById(new CardTagId(1L, 2L))).thenReturn(true);

		// When
		this.cardTagService.deleteCardTag(1L, 2L);

		// Then
		final ArgumentCaptor<CardTagId> captor = ArgumentCaptor.forClass(CardTagId.class);
		verify(this.cardTagRepository).deleteById(captor.capture());
		assertThat(captor.getValue()).isEqualTo(new CardTagId(1L, 2L));
	}

	@Test
	@DisplayName("Should ignore deleting a card tag that does not exist")
	void shouldIgnoreDeletingMissingCardTag() {
		// Given
		when(this.cardTagRepository.existsById(new CardTagId(1L, 2L))).thenReturn(false);

		// When
		this.cardTagService.deleteCardTag(1L, 2L);

		// Then
		verify(this.cardTagRepository, never()).deleteById(any(CardTagId.class));
	}
}
