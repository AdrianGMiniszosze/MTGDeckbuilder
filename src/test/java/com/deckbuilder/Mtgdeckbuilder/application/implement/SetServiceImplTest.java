package com.deckbuilder.Mtgdeckbuilder.application.implement;

import com.deckbuilder.Mtgdeckbuilder.infrastructure.mapper.SetEntityMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Set;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.SetRepository;
import com.deckbuilder.Mtgdeckbuilder.infrastructure.model.SetEntity;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Set Service Tests")
class SetServiceImplTest {

    @Mock
    private SetRepository setRepository;

    @Mock
    private SetEntityMapper setEntityMapper;

    @InjectMocks
    private SetServiceImpl setService;

    private SetEntity testSetEntity;
    private Set testSet;

    @BeforeEach
    void setUp() {
        testSetEntity = new SetEntity();
        testSetEntity.setId(1L);
        testSetEntity.setName("Core Set 2021");

        testSet = new Set();
        testSet.setId(1L);
        testSet.setName("Core Set 2021");
    }

    @Test
    @DisplayName("Should find all sets with pagination")
    void shouldFindAllSets() {
        // Given
        SetEntity set2 = new SetEntity();
        set2.setId(2L);
        set2.setName("Zendikar Rising");

        Set setModel2 = new Set();
        setModel2.setId(2L);
        setModel2.setName("Zendikar Rising");

        Pageable pageable = PageRequest.of(0, 10);
        Page<SetEntity> entityPage = new PageImpl<>(Arrays.asList(testSetEntity, set2));
        when(setRepository.findAll(pageable)).thenReturn(entityPage);
        when(setEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(testSet, setModel2));

        // When
        List<Set> result = setService.findAll(10, 0);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Core Set 2021");
        assertThat(result.get(1).getName()).isEqualTo("Zendikar Rising");
        verify(setRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Should find set by ID when it exists")
    void shouldFindSetById_WhenExists() {
        // Given
        when(setRepository.findById(1L)).thenReturn(Optional.of(testSetEntity));
        when(setEntityMapper.toModel(testSetEntity)).thenReturn(testSet);

        // When
        Optional<Set> result = setService.findById(1L);

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
        Optional<Set> result = setService.findById(999L);

        // Then
        assertThat(result).isEmpty();
        verify(setRepository).findById(999L);
        verify(setEntityMapper, never()).toModel(any(SetEntity.class));
    }

    @Test
    @DisplayName("Should find set by name when it exists")
    void shouldFindSetByName_WhenExists() {
        // Given
        when(setRepository.findByName("Core Set 2021")).thenReturn(Optional.of(testSetEntity));
        when(setEntityMapper.toModel(testSetEntity)).thenReturn(testSet);

        // When
        Optional<Set> result = setService.findByName("Core Set 2021");

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
        Optional<Set> result = setService.findByName("NonExistent");

        // Then
        assertThat(result).isEmpty();
        verify(setRepository).findByName("NonExistent");
        verify(setEntityMapper, never()).toModel(any(SetEntity.class));
    }

    @Test
    @DisplayName("Should create new set")
    void shouldCreateSet() {
        // Given
        when(setEntityMapper.toEntity(testSet)).thenReturn(testSetEntity);
        when(setRepository.save(testSetEntity)).thenReturn(testSetEntity);
        when(setEntityMapper.toModel(testSetEntity)).thenReturn(testSet);

        // When
        Set result = setService.create(testSet);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Core Set 2021");
        verify(setRepository).save(testSetEntity);
    }

    @Test
    @DisplayName("Should update existing set")
    void shouldUpdateSet_WhenExists() {
        // Given
        when(setRepository.existsById(1L)).thenReturn(true);
        when(setEntityMapper.toEntity(testSet)).thenReturn(testSetEntity);
        when(setRepository.save(any(SetEntity.class))).thenReturn(testSetEntity);
        when(setEntityMapper.toModel(testSetEntity)).thenReturn(testSet);

        // When
        Set result = setService.update(1L, testSet);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Core Set 2021");
        verify(setRepository).existsById(1L);
        verify(setRepository).save(any(SetEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent set")
    void shouldThrowException_WhenUpdatingNonExistentSet() {
        // Given
        when(setRepository.existsById(999L)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> setService.update(999L, testSet))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Set not found with id: 999");
        
        verify(setRepository).existsById(999L);
        verify(setRepository, never()).save(any(SetEntity.class));
    }

    @Test
    @DisplayName("Should delete set by ID")
    void shouldDeleteSetById() {
        // Given
        Long setId = 1L;

        // When
        setService.deleteById(setId);

        // Then
        verify(setRepository).deleteById(setId);
    }
}

