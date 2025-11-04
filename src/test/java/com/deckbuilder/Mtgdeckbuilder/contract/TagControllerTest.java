package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.Mtgdeckbuilder.application.TagService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.TagMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Tag;
import com.deckbuilder.apigenerator.openapi.api.model.TagDTO;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Creature");

        testTagDTO = TagDTO.builder()
                .id(1)
                .name("Creature")
                .build();
    }

    @Test
    @DisplayName("Should list all tags with pagination")
    void shouldListAllTags() {
        // Given
        Tag tag2 = new Tag();
        tag2.setId(2L);
        tag2.setName("Instant");

        TagDTO tagDTO2 = TagDTO.builder()
                .id(2)
                .name("Instant")
                .build();

        List<Tag> tags = Arrays.asList(testTag, tag2);
        List<TagDTO> tagDTOs = Arrays.asList(testTagDTO, tagDTO2);

        when(tagService.findAll(10, 0)).thenReturn(tags);
        when(tagMapper.toTagDTOs(tags)).thenReturn(tagDTOs);

        // When
        ResponseEntity<List<TagDTO>> response = tagController.listTags(10, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Creature");
        verify(tagService).findAll(10, 0);
    }

    @Test
    @DisplayName("Should get tag by ID when it exists")
    void shouldGetTagById_WhenExists() {
        // Given
        when(tagService.findById(1L)).thenReturn(Optional.of(testTag));
        when(tagMapper.toTagDTO(testTag)).thenReturn(testTagDTO);

        // When
        ResponseEntity<TagDTO> response = tagController.getTagById(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
        assertThat(response.getBody().getName()).isEqualTo("Creature");
        verify(tagService).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when tag not found")
    void shouldReturn404_WhenTagNotFound() {
        // Given
        when(tagService.findById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<TagDTO> response = tagController.getTagById(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(tagService).findById(999L);
    }

    @Test
    @DisplayName("Should create new tag")
    void shouldCreateTag() {
        // Given
        when(tagMapper.toTag(testTagDTO)).thenReturn(testTag);
        when(tagService.create(testTag)).thenReturn(testTag);
        when(tagMapper.toTagDTO(testTag)).thenReturn(testTagDTO);

        // When
        ResponseEntity<TagDTO> response = tagController.createTag(testTagDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Creature");
        verify(tagService).create(testTag);
    }

    @Test
    @DisplayName("Should update existing tag")
    void shouldUpdateTag() {
        // Given
        when(tagMapper.toTag(testTagDTO)).thenReturn(testTag);
        when(tagService.update(1L, testTag)).thenReturn(testTag);
        when(tagMapper.toTagDTO(testTag)).thenReturn(testTagDTO);

        // When
        ResponseEntity<TagDTO> response = tagController.updateTag(1, testTagDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Creature");
        verify(tagService).update(1L, testTag);
    }

    @Test
    @DisplayName("Should delete tag by ID")
    void shouldDeleteTag() {
        // When
        ResponseEntity<Void> response = tagController.deleteTag(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(tagService).deleteById(1L);
    }

    @Test
    @DisplayName("Should use default pagination when parameters are null")
    void shouldUseDefaultPagination_WhenParametersAreNull() {
        // Given
        when(tagService.findAll(10, 0)).thenReturn(Arrays.asList(testTag));
        when(tagMapper.toTagDTOs(anyList())).thenReturn(Arrays.asList(testTagDTO));

        // When
        ResponseEntity<List<TagDTO>> response = tagController.listTags(null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(tagService).findAll(10, 0);
    }
}
