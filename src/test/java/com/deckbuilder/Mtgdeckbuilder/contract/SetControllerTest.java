package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.SetDTO;
import com.deckbuilder.mtgdeckbuilder.application.SetService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.SetMapper;
import com.deckbuilder.mtgdeckbuilder.model.Set;
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
        this.testSet = new Set();
        this.testSet.setId(1L);
        this.testSet.setName("Core Set 2021");

        this.testSetDTO = SetDTO.builder().id(1).name("Core Set 2021").build();
	}

	@Test
	@DisplayName("Should list all sets with pagination")
	void shouldListAllSets() {
		// Given
		final Set set2 = new Set();
		set2.setId(2L);
		set2.setName("Zendikar Rising");

		final SetDTO setDTO2 = SetDTO.builder().id(2).name("Zendikar Rising").build();

		final List<Set> sets = Arrays.asList(this.testSet, set2);
		final List<SetDTO> setDTOs = Arrays.asList(this.testSetDTO, setDTO2);

		when(this.setService.findAll(10, 0)).thenReturn(sets);
		when(this.setMapper.toSetDTOs(sets)).thenReturn(setDTOs);

		// When
		final ResponseEntity<List<SetDTO>> response = this.setController.listSets(10, 0);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getName()).isEqualTo("Core Set 2021");
		verify(this.setService).findAll(10, 0);
	}

	@Test
	@DisplayName("Should get set by ID when it exists")
	void shouldGetSetById_WhenExists() {
		// Given
		when(this.setService.findById(1L)).thenReturn(Optional.of(this.testSet));
		when(this.setMapper.toSetDTO(this.testSet)).thenReturn(this.testSetDTO);

		// When
		final ResponseEntity<SetDTO> response = this.setController.getSetById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		assertThat(response.getBody().getName()).isEqualTo("Core Set 2021");
		verify(this.setService).findById(1L);
	}

	@Test
	@DisplayName("Should return 404 when set not found")
	void shouldReturn404_WhenSetNotFound() {
		// Given
		when(this.setService.findById(999L)).thenReturn(Optional.empty());

		// When
		final ResponseEntity<SetDTO> response = this.setController.getSetById(999);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
		verify(this.setService).findById(999L);
	}

	@Test
	@DisplayName("Should create new set")
	void shouldCreateSet() {
		// Given
		when(this.setMapper.toSet(this.testSetDTO)).thenReturn(this.testSet);
		when(this.setService.create(this.testSet)).thenReturn(this.testSet);
		when(this.setMapper.toSetDTO(this.testSet)).thenReturn(this.testSetDTO);

		// When
		final ResponseEntity<SetDTO> response = this.setController.createSet(this.testSetDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getName()).isEqualTo("Core Set 2021");
		verify(this.setService).create(this.testSet);
	}

	@Test
	@DisplayName("Should update existing set")
	void shouldUpdateSet() {
		// Given
		when(this.setMapper.toSet(this.testSetDTO)).thenReturn(this.testSet);
		when(this.setService.update(1L, this.testSet)).thenReturn(this.testSet);
		when(this.setMapper.toSetDTO(this.testSet)).thenReturn(this.testSetDTO);

		// When
		final ResponseEntity<SetDTO> response = this.setController.updateSet(1, this.testSetDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getName()).isEqualTo("Core Set 2021");
		verify(this.setService).update(1L, this.testSet);
	}

	@Test
	@DisplayName("Should delete set by ID")
	void shouldDeleteSet() {
		// When
		final ResponseEntity<Void> response = this.setController.deleteSet(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(this.setService).deleteById(1L);
	}

	@Test
	@DisplayName("Should use default pagination when parameters are null")
	void shouldUseDefaultPagination_WhenParametersAreNull() {
		// Given
		when(this.setService.findAll(10, 0)).thenReturn(Collections.singletonList(this.testSet));
		when(this.setMapper.toSetDTOs(anyList())).thenReturn(Collections.singletonList(this.testSetDTO));

		// When
		final ResponseEntity<List<SetDTO>> response = this.setController.listSets(null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(this.setService).findAll(10, 0);
	}
}
