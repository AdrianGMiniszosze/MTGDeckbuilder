package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardTagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardTagEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardTagEntity;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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
		this.testCardTag = CardTag.builder().cardId(1L).tagId(10L).weight(1.0).confidence(1.0).source("scryfall_import")
				.modelVersion("v1.0").createdAt(LocalDateTime.now()).build();

		this.testCardTag2 = CardTag.builder().cardId(1L).tagId(11L).weight(1.0).confidence(0.95)
				.source("scryfall_import").modelVersion("v1.0").createdAt(LocalDateTime.now()).build();

		this.testCardTagEntity = CardTagEntity.builder().cardId(1L).tagId(10L).weight(1.0).confidence(1.0)
				.source("scryfall_import").modelVersion("v1.0").createdAt(LocalDateTime.now()).build();

		this.testCardTagEntity2 = CardTagEntity.builder().cardId(1L).tagId(11L).weight(1.0).confidence(0.95)
				.source("scryfall_import").modelVersion("v1.0").createdAt(LocalDateTime.now()).build();
	}

	@Test
	@DisplayName("Should find all tags for a card")
	void shouldFindAllTagsByCardId() {
		// Given
		when(this.cardTagRepository.findByCardId(1L))
				.thenReturn(Arrays.asList(this.testCardTagEntity, this.testCardTagEntity2));
		when(this.cardTagEntityMapper.toModelList(anyList()))
				.thenReturn(Arrays.asList(this.testCardTag, this.testCardTag2));

		// When
		final List<CardTag> result = this.cardTagService.findByCardId(1L);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(CardTag::getCardId).containsOnly(1L);
		assertThat(result).extracting(CardTag::getTagId).containsExactly(10L, 11L);
		verify(this.cardTagRepository, times(1)).findByCardId(1L);
		verify(this.cardTagEntityMapper, times(1)).toModelList(anyList());
	}

	@Test
	@DisplayName("Should return empty list when card has no tags")
	void shouldReturnEmptyListWhenCardHasNoTags() {
		// Given
		when(this.cardTagRepository.findByCardId(999L)).thenReturn(List.of());
		when(this.cardTagEntityMapper.toModelList(anyList())).thenReturn(List.of());

		// When
		final List<CardTag> result = this.cardTagService.findByCardId(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.cardTagRepository, times(1)).findByCardId(999L);
		verify(this.cardTagEntityMapper, times(1)).toModelList(anyList());
	}

	@Test
	@DisplayName("Should return tags with different confidence levels")
	void shouldReturnTagsWithDifferentConfidenceLevels() {
		// Given
		when(this.cardTagRepository.findByCardId(1L))
				.thenReturn(Arrays.asList(this.testCardTagEntity, this.testCardTagEntity2));
		when(this.cardTagEntityMapper.toModelList(anyList()))
				.thenReturn(Arrays.asList(this.testCardTag, this.testCardTag2));

		// When
		final List<CardTag> result = this.cardTagService.findByCardId(1L);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).extracting(CardTag::getConfidence).containsExactly(1.0, 0.95);
		assertThat(result.get(0).getConfidence()).isEqualTo(1.0);
		assertThat(result.get(1).getConfidence()).isEqualTo(0.95);
	}

	@Test
	@DisplayName("Should return tags with correct source information")
	void shouldReturnTagsWithCorrectSource() {
		// Given
		when(this.cardTagRepository.findByCardId(1L)).thenReturn(Collections.singletonList(this.testCardTagEntity));
		when(this.cardTagEntityMapper.toModelList(anyList())).thenReturn(Collections.singletonList(this.testCardTag));

		// When
		final List<CardTag> result = this.cardTagService.findByCardId(1L);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getSource()).isEqualTo("scryfall_import");
		assertThat(result.get(0).getModelVersion()).isEqualTo("v1.0");
	}

	@Test
	@DisplayName("Should handle multiple tags for single card")
	void shouldHandleMultipleTagsForSingleCard() {
		// Given
		final CardTagEntity tag3 = CardTagEntity.builder().cardId(1L).tagId(12L).weight(1.0).confidence(0.85)
				.source("ml_model").modelVersion("v2.0").createdAt(LocalDateTime.now()).build();

		final CardTag modelTag3 = CardTag.builder().cardId(1L).tagId(12L).weight(1.0).confidence(0.85)
				.source("ml_model").modelVersion("v2.0").createdAt(LocalDateTime.now()).build();

		when(this.cardTagRepository.findByCardId(1L))
				.thenReturn(Arrays.asList(this.testCardTagEntity, this.testCardTagEntity2, tag3));
		when(this.cardTagEntityMapper.toModelList(anyList()))
				.thenReturn(Arrays.asList(this.testCardTag, this.testCardTag2, modelTag3));

		// When
		final List<CardTag> result = this.cardTagService.findByCardId(1L);

		// Then
		assertThat(result).hasSize(3);
		assertThat(result).extracting(CardTag::getTagId).containsExactly(10L, 11L, 12L);
		assertThat(result).extracting(CardTag::getSource).containsExactly("scryfall_import", "scryfall_import",
				"ml_model");
	}

	@Test
	@DisplayName("Should verify mapper is called correctly")
	void shouldVerifyMapperIsCalledCorrectly() {
		// Given
		final List<CardTagEntity> entities = Collections.singletonList(this.testCardTagEntity);
		when(this.cardTagRepository.findByCardId(1L)).thenReturn(entities);
		when(this.cardTagEntityMapper.toModelList(entities)).thenReturn(Collections.singletonList(this.testCardTag));

		// When
		this.cardTagService.findByCardId(1L);

		// Then
		verify(this.cardTagEntityMapper, times(1)).toModelList(entities);
	}
}
