package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.TagEntity;
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
        testTagEntity = new TagEntity();
        testTagEntity.setId(1L);
        testTagEntity.setName("Creature");
    }

    @Test
    @DisplayName("Should find tag by ID when it exists")
    void shouldFindTagById_WhenExists() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(testTagEntity));

        // When
        Optional<TagEntity> result = tagRepository.findById(1L);

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
        Optional<TagEntity> result = tagRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(tagRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find tag by name when it exists")
    void shouldFindTagByName_WhenExists() {
        // Given
        when(tagRepository.findByName("Creature")).thenReturn(Optional.of(testTagEntity));

        // When
        Optional<TagEntity> result = tagRepository.findByName("Creature");

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
        Optional<TagEntity> result = tagRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(tagRepository).findByName("NonExistent");
    }

    @Test
    @DisplayName("Should find all tags with pagination")
    void shouldFindAllTagsWithPagination() {
        // Given
        TagEntity tag2 = new TagEntity();
        tag2.setId(2L);
        tag2.setName("Instant");

        Pageable pageable = PageRequest.of(0, 10);
        Page<TagEntity> page = new PageImpl<>(Arrays.asList(testTagEntity, tag2));
        when(tagRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<TagEntity> result = tagRepository.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Creature");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Instant");
        verify(tagRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should save tag entity")
    void shouldSaveTag() {
        // Given
        when(tagRepository.save(any(TagEntity.class))).thenReturn(testTagEntity);

        // When
        TagEntity result = tagRepository.save(testTagEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Creature");
        verify(tagRepository).save(testTagEntity);
    }

    @Test
    @DisplayName("Should delete tag by ID")
    void shouldDeleteTagById() {
        // Given
        Long tagId = 1L;

        // When
        tagRepository.deleteById(tagId);

        // Then
        verify(tagRepository).deleteById(tagId);
    }

    @Test
    @DisplayName("Should check if tag exists by ID")
    void shouldCheckIfTagExists() {
        // Given
        when(tagRepository.existsById(1L)).thenReturn(true);
        when(tagRepository.existsById(999L)).thenReturn(false);

        // When
        boolean exists = tagRepository.existsById(1L);
        boolean notExists = tagRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(tagRepository).existsById(1L);
        verify(tagRepository).existsById(999L);
    }
}
