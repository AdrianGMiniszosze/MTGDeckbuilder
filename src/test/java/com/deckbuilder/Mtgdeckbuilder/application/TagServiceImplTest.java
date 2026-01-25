package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.TagServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.TagNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.TagEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.TagEntity;
import com.deckbuilder.mtgdeckbuilder.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tag Service Tests")
class TagServiceImplTest {

	@Mock
	private TagRepository tagRepository;

	@Mock
	private TagEntityMapper tagEntityMapper;

	@InjectMocks
	private TagServiceImpl tagService;

	private TagEntity testTagEntity;
	private Tag testTag;

	@BeforeEach
	void setUp() {
		this.testTagEntity = new TagEntity();
		this.testTagEntity.setId(1L);
		this.testTagEntity.setName("Creature");

		this.testTag = new Tag();
		this.testTag.setId(1L);
		this.testTag.setName("Creature");
	}

	@Test
	@DisplayName("Should find all tags with pagination")
	void shouldFindAllTags() {
		// Given
		final TagEntity tag2 = new TagEntity();
		tag2.setId(2L);
		tag2.setName("Instant");

		final Tag tagModel2 = new Tag();
		tagModel2.setId(2L);
		tagModel2.setName("Instant");

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<TagEntity> entityPage = new PageImpl<>(Arrays.asList(this.testTagEntity, tag2));
		when(this.tagRepository.findAll(pageable)).thenReturn(entityPage);
		when(this.tagEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(this.testTag, tagModel2));

		// When
		final List<Tag> result = this.tagService.findAll(10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Creature");
		assertThat(result.get(1).getName()).isEqualTo("Instant");
		verify(this.tagRepository).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should find tag by ID when it exists")
	void shouldFindTagById_WhenExists() {
		// Given
		when(this.tagRepository.findById(1L)).thenReturn(Optional.of(this.testTagEntity));
		when(this.tagEntityMapper.toModel(this.testTagEntity)).thenReturn(this.testTag);

		// When
		final Optional<Tag> result = this.tagService.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Creature");
		verify(this.tagRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when tag ID does not exist")
	void shouldReturnEmpty_WhenTagNotFound() {
		// Given
		when(this.tagRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Tag> result = this.tagService.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.tagRepository).findById(999L);
		verify(this.tagEntityMapper, never()).toModel(any(TagEntity.class));
	}

	@Test
	@DisplayName("Should find tag by name when it exists")
	void shouldFindTagByName_WhenExists() {
		// Given
		when(this.tagRepository.findByName("Creature")).thenReturn(Optional.of(this.testTagEntity));
		when(this.tagEntityMapper.toModel(this.testTagEntity)).thenReturn(this.testTag);

		// When
		final Optional<Tag> result = this.tagService.findByName("Creature");

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("Creature");
		verify(this.tagRepository).findByName("Creature");
	}

	@Test
	@DisplayName("Should return empty when tag name does not exist")
	void shouldReturnEmpty_WhenTagNameNotFound() {
		// Given
		when(this.tagRepository.findByName("NonExistent")).thenReturn(Optional.empty());

		// When
		final Optional<Tag> result = this.tagService.findByName("NonExistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.tagRepository).findByName("NonExistent");
		verify(this.tagEntityMapper, never()).toModel(any(TagEntity.class));
	}

	@Test
	@DisplayName("Should create new tag")
	void shouldCreateTag() {
		// Given
		when(this.tagEntityMapper.toEntity(this.testTag)).thenReturn(this.testTagEntity);
		when(this.tagRepository.save(this.testTagEntity)).thenReturn(this.testTagEntity);
		when(this.tagEntityMapper.toModel(this.testTagEntity)).thenReturn(this.testTag);

		// When
		final Tag result = this.tagService.create(this.testTag);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Creature");
		verify(this.tagRepository).save(this.testTagEntity);
	}

	@Test
	@DisplayName("Should update existing tag")
	void shouldUpdateTag_WhenExists() {
		// Given
		when(this.tagRepository.existsById(1L)).thenReturn(true);
		when(this.tagEntityMapper.toEntity(this.testTag)).thenReturn(this.testTagEntity);
		when(this.tagRepository.save(any(TagEntity.class))).thenReturn(this.testTagEntity);
		when(this.tagEntityMapper.toModel(this.testTagEntity)).thenReturn(this.testTag);

		// When
		final Tag result = this.tagService.update(1L, this.testTag);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Creature");
		verify(this.tagRepository).existsById(1L);
		verify(this.tagRepository).save(any(TagEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent tag")
	void shouldThrowException_WhenUpdatingNonExistentTag() {
		// Given
		when(this.tagRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.tagService.update(999L, this.testTag)).isInstanceOf(TagNotFoundException.class)
				.hasMessageContaining("Tag not found with id: 999");

		verify(this.tagRepository).existsById(999L);
		verify(this.tagRepository, never()).save(any(TagEntity.class));
	}

	@Test
	@DisplayName("Should delete tag by ID")
	void shouldDeleteTagById() {
		// Given
		final Long tagId = 1L;

		// When
		this.tagService.deleteById(tagId);

		// Then
		verify(this.tagRepository).deleteById(tagId);
	}
}
