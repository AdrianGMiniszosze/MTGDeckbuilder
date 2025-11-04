package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.Mtgdeckbuilder.application.SetService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.SetMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Set;
import com.deckbuilder.apigenerator.openapi.api.model.SetDTO;
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
@DisplayName("Set Controller Tests")
class SetControllerTest {

    @Mock
    private SetService setService;

    @Mock
    private SetMapper setMapper;

    @InjectMocks
    private SetController setController;

    private Set testSet;
    private SetDTO testSetDTO;

    @BeforeEach
    void setUp() {
        testSet = new Set();
        testSet.setId(1L);
        testSet.setName("Core Set 2021");

        testSetDTO = SetDTO.builder()
                .id(1)
                .name("Core Set 2021")
                .build();
    }

    @Test
    @DisplayName("Should list all sets with pagination")
    void shouldListAllSets() {
        // Given
        Set set2 = new Set();
        set2.setId(2L);
        set2.setName("Zendikar Rising");

        SetDTO setDTO2 = SetDTO.builder()
                .id(2)
                .name("Zendikar Rising")
                .build();

        List<Set> sets = Arrays.asList(testSet, set2);
        List<SetDTO> setDTOs = Arrays.asList(testSetDTO, setDTO2);

        when(setService.findAll(10, 0)).thenReturn(sets);
        when(setMapper.toSetDTOs(sets)).thenReturn(setDTOs);

        // When
        ResponseEntity<List<SetDTO>> response = setController.listSets(10, 0);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getName()).isEqualTo("Core Set 2021");
        verify(setService).findAll(10, 0);
    }

    @Test
    @DisplayName("Should get set by ID when it exists")
    void shouldGetSetById_WhenExists() {
        // Given
        when(setService.findById(1L)).thenReturn(Optional.of(testSet));
        when(setMapper.toSetDTO(testSet)).thenReturn(testSetDTO);

        // When
        ResponseEntity<SetDTO> response = setController.getSetById(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
        assertThat(response.getBody().getName()).isEqualTo("Core Set 2021");
        verify(setService).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when set not found")
    void shouldReturn404_WhenSetNotFound() {
        // Given
        when(setService.findById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<SetDTO> response = setController.getSetById(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(setService).findById(999L);
    }

    @Test
    @DisplayName("Should create new set")
    void shouldCreateSet() {
        // Given
        when(setMapper.toSet(testSetDTO)).thenReturn(testSet);
        when(setService.create(testSet)).thenReturn(testSet);
        when(setMapper.toSetDTO(testSet)).thenReturn(testSetDTO);

        // When
        ResponseEntity<SetDTO> response = setController.createSet(testSetDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Core Set 2021");
        verify(setService).create(testSet);
    }

    @Test
    @DisplayName("Should update existing set")
    void shouldUpdateSet() {
        // Given
        when(setMapper.toSet(testSetDTO)).thenReturn(testSet);
        when(setService.update(1L, testSet)).thenReturn(testSet);
        when(setMapper.toSetDTO(testSet)).thenReturn(testSetDTO);

        // When
        ResponseEntity<SetDTO> response = setController.updateSet(1, testSetDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Core Set 2021");
        verify(setService).update(1L, testSet);
    }

    @Test
    @DisplayName("Should delete set by ID")
    void shouldDeleteSet() {
        // When
        ResponseEntity<Void> response = setController.deleteSet(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(setService).deleteById(1L);
    }

    @Test
    @DisplayName("Should use default pagination when parameters are null")
    void shouldUseDefaultPagination_WhenParametersAreNull() {
        // Given
        when(setService.findAll(10, 0)).thenReturn(Arrays.asList(testSet));
        when(setMapper.toSetDTOs(anyList())).thenReturn(Arrays.asList(testSetDTO));

        // When
        ResponseEntity<List<SetDTO>> response = setController.listSets(null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(setService).findAll(10, 0);
    }
}
