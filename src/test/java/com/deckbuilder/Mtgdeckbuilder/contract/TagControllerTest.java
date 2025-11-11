package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.TagDTO;
import com.deckbuilder.mtgdeckbuilder.application.TagService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.TagMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.TagNotFoundException;
import com.deckbuilder.mtgdeckbuilder.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tag Controller Tests")
class TagControllerTest {

	@Mock
	private TagService tagService;

	@Mock
	private TagMapper tagMapper;

	@InjectMocks
	private TagController tagController;

	private Tag testTag;
	private TagDTO testTagDTO;

	@BeforeEach
	void setUp() {
		this.testTag = new Tag();
		this.testTag.setId(1L);
		this.testTag.setName("Creature");

		this.testTagDTO = TagDTO.builder().id(1).name("Creature").build();
	}

	@Test
	@DisplayName("Should list all tags with pagination")
	void shouldListAllTags() {
		// Given
		final Tag tag2 = new Tag();
		tag2.setId(2L);
		tag2.setName("Instant");

		final TagDTO tagDTO2 = TagDTO.builder().id(2).name("Instant").build();

		final List<Tag> tags = Arrays.asList(this.testTag, tag2);
		final List<TagDTO> tagDTOs = Arrays.asList(this.testTagDTO, tagDTO2);

		when(this.tagService.findAll(10, 0)).thenReturn(tags);
		when(this.tagMapper.toTagDTOs(tags)).thenReturn(tagDTOs);

		// When
		final ResponseEntity<List<TagDTO>> response = this.tagController.listTags(10, 0);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getName()).isEqualTo("Creature");
		verify(this.tagService).findAll(10, 0);
	}

	@Test
	@DisplayName("Should get tag by ID when it exists")
	void shouldGetTagById_WhenExists() {
		// Given
		when(this.tagService.findById(1L)).thenReturn(Optional.of(this.testTag));
		when(this.tagMapper.toTagDTO(this.testTag)).thenReturn(this.testTagDTO);

		// When
		final ResponseEntity<TagDTO> response = this.tagController.getTagById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		assertThat(response.getBody().getName()).isEqualTo("Creature");
		verify(this.tagService).findById(1L);
	}

	@Test
	@DisplayName("Should throw exception when tag not found")
	void shouldReturn404_WhenTagNotFound() {
		// Given
		when(this.tagService.findById(999L)).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.tagController.getTagById(999)).isInstanceOf(TagNotFoundException.class)
				.hasMessageContaining("Tag not found with id: 999");

		verify(this.tagService).findById(999L);
	}

	@Test
	@DisplayName("Should create new tag")
	void shouldCreateTag() {
		// Given
		when(this.tagMapper.toTag(this.testTagDTO)).thenReturn(this.testTag);
		when(this.tagService.create(this.testTag)).thenReturn(this.testTag);
		when(this.tagMapper.toTagDTO(this.testTag)).thenReturn(this.testTagDTO);

		// When
		final ResponseEntity<TagDTO> response = this.tagController.createTag(this.testTagDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getName()).isEqualTo("Creature");
		verify(this.tagService).create(this.testTag);
	}

	@Test
	@DisplayName("Should update existing tag")
	void shouldUpdateTag() {
		// Given
		when(this.tagMapper.toTag(this.testTagDTO)).thenReturn(this.testTag);
		when(this.tagService.update(1L, this.testTag)).thenReturn(this.testTag);
		when(this.tagMapper.toTagDTO(this.testTag)).thenReturn(this.testTagDTO);

		// When
		final ResponseEntity<TagDTO> response = this.tagController.updateTag(1, this.testTagDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getName()).isEqualTo("Creature");
		verify(this.tagService).update(1L, this.testTag);
	}

	@Test
	@DisplayName("Should delete tag by ID")
	void shouldDeleteTag() {
		// When
		final ResponseEntity<Void> response = this.tagController.deleteTag(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(this.tagService).deleteById(1L);
	}

	@Test
	@DisplayName("Should use default pagination when parameters are null")
	void shouldUseDefaultPagination_WhenParametersAreNull() {
		// Given
		when(this.tagService.findAll(10, 0)).thenReturn(Collections.singletonList(this.testTag));
		when(this.tagMapper.toTagDTOs(anyList())).thenReturn(Collections.singletonList(this.testTagDTO));

		// When
		final ResponseEntity<List<TagDTO>> response = this.tagController.listTags(null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.tagService).findAll(10, 0);
	}
}
