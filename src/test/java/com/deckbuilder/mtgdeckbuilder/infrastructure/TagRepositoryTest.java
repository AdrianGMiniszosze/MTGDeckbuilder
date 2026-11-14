package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.TagEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tag Repository Tests")
class TagRepositoryTest {

	@Mock
	private TagRepository tagRepository;

	private TagEntity testTagEntity;

	@BeforeEach
	void setUp() {
        this.testTagEntity = new TagEntity();
        this.testTagEntity.setId(1L);
        this.testTagEntity.setName("Creature");
	}

	@Test
	@DisplayName("Should find tag by ID when it exists")
	void shouldFindTagById_WhenExists() {
		// Given
		when(this.tagRepository.findById(1L)).thenReturn(Optional.of(this.testTagEntity));

		// When
		final Optional<TagEntity> result = this.tagRepository.findById(1L);

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
		final Optional<TagEntity> result = this.tagRepository.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.tagRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find tag by name when it exists")
	void shouldFindTagByName_WhenExists() {
		// Given
		when(this.tagRepository.findByName("Creature")).thenReturn(Optional.of(this.testTagEntity));

		// When
		final Optional<TagEntity> result = this.tagRepository.findByName("Creature");

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
		final Optional<TagEntity> result = this.tagRepository.findByName("NonExistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.tagRepository).findByName("NonExistent");
	}

	@Test
	@DisplayName("Should find all tags with pagination")
	void shouldFindAllTagsWithPagination() {
		// Given
		final TagEntity tag2 = new TagEntity();
		tag2.setId(2L);
		tag2.setName("Instant");

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<TagEntity> page = new PageImpl<>(Arrays.asList(this.testTagEntity, tag2));
		when(this.tagRepository.findAll(pageable)).thenReturn(page);

		// When
		final Page<TagEntity> result = this.tagRepository.findAll(pageable);

		// Then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0).getName()).isEqualTo("Creature");
		assertThat(result.getContent().get(1).getName()).isEqualTo("Instant");
		verify(this.tagRepository).findAll(pageable);
	}

	@Test
	@DisplayName("Should save tag entity")
	void shouldSaveTag() {
		// Given
		when(this.tagRepository.save(any(TagEntity.class))).thenReturn(this.testTagEntity);

		// When
		final TagEntity result = this.tagRepository.save(this.testTagEntity);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Creature");
		verify(this.tagRepository).save(this.testTagEntity);
	}

	@Test
	@DisplayName("Should delete tag by ID")
	void shouldDeleteTagById() {
		// Given
		final Long tagId = 1L;

		// When
        this.tagRepository.deleteById(tagId);

		// Then
		verify(this.tagRepository).deleteById(tagId);
	}

	@Test
	@DisplayName("Should check if tag exists by ID")
	void shouldCheckIfTagExists() {
		// Given
		when(this.tagRepository.existsById(1L)).thenReturn(true);
		when(this.tagRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean exists = this.tagRepository.existsById(1L);
		final boolean notExists = this.tagRepository.existsById(999L);

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.tagRepository).existsById(1L);
		verify(this.tagRepository).existsById(999L);
	}
}
