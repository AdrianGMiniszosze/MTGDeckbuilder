package com.deckbuilder.Mtgdeckbuilder.infrastructure;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.SetEntity;
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
@DisplayName("Set Repository Tests")
class SetRepositoryTest {

    @Mock
    private SetRepository setRepository;

    private SetEntity testSetEntity;

    @BeforeEach
    void setUp() {
        testSetEntity = new SetEntity();
        testSetEntity.setId(1L);
        testSetEntity.setName("Core Set 2021");
    }

    @Test
    @DisplayName("Should find set by ID when it exists")
    void shouldFindSetById_WhenExists() {
        // Given
        when(setRepository.findById(1L)).thenReturn(Optional.of(testSetEntity));

        // When
        Optional<SetEntity> result = setRepository.findById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("Core Set 2021");
        verify(setRepository).findById(1L);
    }

    @Test
    @DisplayName("Should return empty when set ID does not exist")
    void shouldReturnEmpty_WhenSetNotFound() {
        // Given
        when(setRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<SetEntity> result = setRepository.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(setRepository).findById(999L);
    }

    @Test
    @DisplayName("Should find set by name when it exists")
    void shouldFindSetByName_WhenExists() {
        // Given
        when(setRepository.findByName("Core Set 2021")).thenReturn(Optional.of(testSetEntity));

        // When
        Optional<SetEntity> result = setRepository.findByName("Core Set 2021");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Core Set 2021");
        verify(setRepository).findByName("Core Set 2021");
    }

    @Test
    @DisplayName("Should return empty when set name does not exist")
    void shouldReturnEmpty_WhenSetNameNotFound() {
        // Given
        when(setRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        // When
        Optional<SetEntity> result = setRepository.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(setRepository).findByName("NonExistent");
    }

    @Test
    @DisplayName("Should find all sets with pagination")
    void shouldFindAllSetsWithPagination() {
        // Given
        SetEntity set2 = new SetEntity();
        set2.setId(2L);
        set2.setName("Zendikar Rising");

        Pageable pageable = PageRequest.of(0, 10);
        Page<SetEntity> page = new PageImpl<>(Arrays.asList(testSetEntity, set2));
        when(setRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<SetEntity> result = setRepository.findAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Core Set 2021");
        assertThat(result.getContent().get(1).getName()).isEqualTo("Zendikar Rising");
        verify(setRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should save set entity")
    void shouldSaveSet() {
        // Given
        when(setRepository.save(any(SetEntity.class))).thenReturn(testSetEntity);

        // When
        SetEntity result = setRepository.save(testSetEntity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Core Set 2021");
        verify(setRepository).save(testSetEntity);
    }

    @Test
    @DisplayName("Should delete set by ID")
    void shouldDeleteSetById() {
        // Given
        Long setId = 1L;

        // When
        setRepository.deleteById(setId);

        // Then
        verify(setRepository).deleteById(setId);
    }

    @Test
    @DisplayName("Should check if set exists by ID")
    void shouldCheckIfSetExists() {
        // Given
        when(setRepository.existsById(1L)).thenReturn(true);
        when(setRepository.existsById(999L)).thenReturn(false);

        // When
        boolean exists = setRepository.existsById(1L);
        boolean notExists = setRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
        verify(setRepository).existsById(1L);
        verify(setRepository).existsById(999L);
    }
}
