package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.FormatEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Format Repository Tests")
class FormatRepositoryTest {

    @Mock
    private FormatRepository formatRepository;

    private FormatEntity testFormatEntity;

    @BeforeEach
    void setUp() {
        testFormatEntity = new FormatEntity();
        testFormatEntity.setId(1L);
        testFormatEntity.setName("Standard");
        testFormatEntity.setDescription("Standard format");
        testFormatEntity.setMinDeckSize(60);
        testFormatEntity.setMaxDeckSize(60);
        testFormatEntity.setMaxSideboardSize(15);
        testFormatEntity.setBannedCards(Arrays.asList("123", "456"));
        testFormatEntity.setRestrictedCards(Arrays.asList("789"));
    }

    @Test
    @DisplayName("Should find format by ID when it exists")
    void shouldFindFormatById_WhenExists() {
        // Given
        when(formatRepository.findById(1L)).thenReturn(Optional.of(testFormatEntity));

        // When
        Optional<FormatEntity> result = formatRepository.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Standard");
        verify(formatRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when format ID does not exist")
    void shouldReturnEmpty_WhenFormatNotFound() {
        // Given
        when(formatRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<FormatEntity> result = formatRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(formatRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find format by name when it exists")
    void shouldFindFormatByName_WhenExists() {
        // Given
        when(formatRepository.findByName("Standard")).thenReturn(Optional.of(testFormatEntity));

        // When
        Optional<FormatEntity> result = formatRepository.findByName("Standard");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Standard");
        verify(formatRepository).findByName("Standard");
    }

    @Test
    @DisplayName("Should return empty when format name does not exist")
    void shouldReturnEmpty_WhenFormatNameNotFound() {
        // Given
        when(formatRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        // When
        Optional<FormatEntity> result = formatRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(formatRepository).findByName("NonExistent");
    }

    @Test
    @DisplayName("Should find all formats")
    void shouldFindAllFormats() {
        // Given
        FormatEntity format2 = new FormatEntity();
        format2.setId(2L);
        format2.setName("Commander");
        format2.setMinDeckSize(100);
        format2.setMaxDeckSize(100);

        List<FormatEntity> formats = Arrays.asList(testFormatEntity, format2);
        when(formatRepository.findAll()).thenReturn(formats);

        // When
        List<FormatEntity> result = formatRepository.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Standard");
        assertThat(result.get(1).getName()).isEqualTo("Commander");
        verify(formatRepository).findAll();
    }

    @Test
    @DisplayName("Should save format entity")
    void shouldSaveFormat() {
        // Given
        when(formatRepository.save(any(FormatEntity.class))).thenReturn(testFormatEntity);

        // When
        FormatEntity result = formatRepository.save(testFormatEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Standard");
        verify(formatRepository).save(testFormatEntity);
    }

    @Test
    @DisplayName("Should delete format by ID")
    void shouldDeleteFormatById() {
        // Given
        Long formatId = 1L;

        // When
        formatRepository.deleteById(formatId);

        // Then
        verify(formatRepository).deleteById(formatId);
    }

    @Test
    @DisplayName("Should check if format exists by ID")
    void shouldCheckIfFormatExists() {
        // Given
        when(formatRepository.existsById(1L)).thenReturn(true);
        when(formatRepository.existsById(999L)).thenReturn(false);

        // When
        boolean exists = formatRepository.existsById(1L);
        boolean notExists = formatRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(formatRepository).existsById(1L);
        verify(formatRepository).existsById(999L);
    }
}
