package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.TagEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Tag;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.TagRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.TagEntity;
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
        testTagEntity = new TagEntity();
        testTagEntity.setId(1L);
        testTagEntity.setName("Creature");

        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Creature");
    }

    @Test
    @DisplayName("Should find all tags with pagination")
    void shouldFindAllTags() {
        // Given
        TagEntity tag2 = new TagEntity();
        tag2.setId(2L);
        tag2.setName("Instant");

        Tag tagModel2 = new Tag();
        tagModel2.setId(2L);
        tagModel2.setName("Instant");

        Pageable pageable = PageRequest.of(0, 10);
        Page<TagEntity> entityPage = new PageImpl<>(Arrays.asList(testTagEntity, tag2));
        when(tagRepository.findAll(pageable)).thenReturn(entityPage);
        when(tagEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testTag, tagModel2));

        // When
        List<Tag> result = tagService.findAll(10, 0);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Creature");
        assertThat(result.get(1).getName()).isEqualTo("Instant");
        verify(tagRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should find tag by ID when it exists")
    void shouldFindTagById_WhenExists() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTagEntity));
        when(tagEntityMapper.toModel(testTagEntity)).thenReturn(testTag);

        // When
        Optional<Tag> result = tagService.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Creature");
        verify(tagRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when tag ID does not exist")
    void shouldReturnEmpty_WhenTagNotFound() {
        // Given
        when(tagRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Tag> result = tagService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(tagRepository).findById(999L);
        verify(tagEntityMapper, never()).toModel(any(TagEntity.class));
    }

    @Test
    @DisplayName("Should find tag by name when it exists")
    void shouldFindTagByName_WhenExists() {
        // Given
        when(tagRepository.findByName("Creature")).thenReturn(Optional.of(testTagEntity));
        when(tagEntityMapper.toModel(testTagEntity)).thenReturn(testTag);

        // When
        Optional<Tag> result = tagService.findByName("Creature");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Creature");
        verify(tagRepository).findByName("Creature");
    }

    @Test
    @DisplayName("Should return empty when tag name does not exist")
    void shouldReturnEmpty_WhenTagNameNotFound() {
        // Given
        when(tagRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        // When
        Optional<Tag> result = tagService.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(tagRepository).findByName("NonExistent");
        verify(tagEntityMapper, never()).toModel(any(TagEntity.class));
    }
    
    @Test
    @DisplayName("Should create new tag")
    void shouldCreateTag() {
        // Given
        when(tagEntityMapper.toEntity(testTag)).thenReturn(testTagEntity);
        when(tagRepository.save(testTagEntity)).thenReturn(testTagEntity);
        when(tagEntityMapper.toModel(testTagEntity)).thenReturn(testTag);

        // When
        Tag result = tagService.create(testTag);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Creature");
        verify(tagRepository).save(testTagEntity);
    }

    @Test
    @DisplayName("Should update existing tag")
    void shouldUpdateTag_WhenExists() {
        // Given
        when(tagRepository.existsById(1L)).thenReturn(true);
        when(tagEntityMapper.toEntity(testTag)).thenReturn(testTagEntity);
        when(tagRepository.save(any(TagEntity.class))).thenReturn(testTagEntity);
        when(tagEntityMapper.toModel(testTagEntity)).thenReturn(testTag);

        // When
        Tag result = tagService.update(1L, testTag);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Creature");
        verify(tagRepository).existsById(1L);
        verify(tagRepository).save(any(TagEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent tag")
    void shouldThrowException_WhenUpdatingNonExistentTag() {
        // Given
        when(tagRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> tagService.update(999L, testTag))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tag not found with id: 999");
        
        verify(tagRepository).existsById(999L);
        verify(tagRepository, never()).save(any(TagEntity.class));
    }

    @Test
    @DisplayName("Should delete tag by ID")
    void shouldDeleteTagById() {
        // Given
        Long tagId = 1L;

        // When
        tagService.deleteById(tagId);

        // Then
        verify(tagRepository).deleteById(tagId);
    }
}
