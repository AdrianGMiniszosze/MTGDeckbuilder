package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.SetRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.SetNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.SetEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.SetEntity;
import com.deckbuilder.mtgdeckbuilder.model.Set;
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
		this.testSetEntity = new SetEntity();
		this.testSetEntity.setId(1L);
		this.testSetEntity.setName("Core Set 2021");

		this.testSet = new Set();
		this.testSet.setId(1L);
		this.testSet.setName("Core Set 2021");
	}

	@Test
	@DisplayName("Should find all sets with pagination")
	void shouldFindAllSets() {
		// Given
		final SetEntity set2 = new SetEntity();
		set2.setId(2L);
		set2.setName("Zendikar Rising");

		final Set setModel2 = new Set();
		setModel2.setId(2L);
		setModel2.setName("Zendikar Rising");

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<SetEntity> entityPage = new PageImpl<>(Arrays.asList(this.testSetEntity, set2));
		when(this.setRepository.findAll(pageable)).thenReturn(entityPage);
		when(this.setEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(this.testSet, setModel2));

		// When
		final List<Set> result = this.setService.findAll(10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Core Set 2021");
		assertThat(result.get(1).getName()).isEqualTo("Zendikar Rising");
		verify(this.setRepository).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should find set by ID when it exists")
	void shouldFindSetById_WhenExists() {
		// Given
		when(this.setRepository.findById(1L)).thenReturn(Optional.of(this.testSetEntity));
		when(this.setEntityMapper.toModel(this.testSetEntity)).thenReturn(this.testSet);

		// When
		final Optional<Set> result = this.setService.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Core Set 2021");
		verify(this.setRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when set ID does not exist")
	void shouldReturnEmpty_WhenSetNotFound() {
		// Given
		when(this.setRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Set> result = this.setService.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.setRepository).findById(999L);
		verify(this.setEntityMapper, never()).toModel(any(SetEntity.class));
	}

	@Test
	@DisplayName("Should find set by name when it exists")
	void shouldFindSetByName_WhenExists() {
		// Given
		when(this.setRepository.findByName("Core Set 2021")).thenReturn(Optional.of(this.testSetEntity));
		when(this.setEntityMapper.toModel(this.testSetEntity)).thenReturn(this.testSet);

		// When
		final Optional<Set> result = this.setService.findByName("Core Set 2021");

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("Core Set 2021");
		verify(this.setRepository).findByName("Core Set 2021");
	}

	@Test
	@DisplayName("Should return empty when set name does not exist")
	void shouldReturnEmpty_WhenSetNameNotFound() {
		// Given
		when(this.setRepository.findByName("NonExistent")).thenReturn(Optional.empty());

		// When
		final Optional<Set> result = this.setService.findByName("NonExistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.setRepository).findByName("NonExistent");
		verify(this.setEntityMapper, never()).toModel(any(SetEntity.class));
	}

	@Test
	@DisplayName("Should create new set")
	void shouldCreateSet() {
		// Given
		when(this.setEntityMapper.toEntity(this.testSet)).thenReturn(this.testSetEntity);
		when(this.setRepository.save(this.testSetEntity)).thenReturn(this.testSetEntity);
		when(this.setEntityMapper.toModel(this.testSetEntity)).thenReturn(this.testSet);

		// When
		final Set result = this.setService.create(this.testSet);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Core Set 2021");
		verify(this.setRepository).save(this.testSetEntity);
	}

	@Test
	@DisplayName("Should update existing set")
	void shouldUpdateSet_WhenExists() {
		// Given
		when(this.setRepository.existsById(1L)).thenReturn(true);
		when(this.setEntityMapper.toEntity(this.testSet)).thenReturn(this.testSetEntity);
		when(this.setRepository.save(any(SetEntity.class))).thenReturn(this.testSetEntity);
		when(this.setEntityMapper.toModel(this.testSetEntity)).thenReturn(this.testSet);

		// When
		final Set result = this.setService.update(1L, this.testSet);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Core Set 2021");
		verify(this.setRepository).existsById(1L);
		verify(this.setRepository).save(any(SetEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent set")
	void shouldThrowException_WhenUpdatingNonExistentSet() {
		// Given
		when(this.setRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.setService.update(999L, this.testSet)).isInstanceOf(SetNotFoundException.class)
				.hasMessageContaining("Set not found with id: 999");

		verify(this.setRepository).existsById(999L);
		verify(this.setRepository, never()).save(any(SetEntity.class));
	}

	@Test
	@DisplayName("Should delete set by ID")
	void shouldDeleteSetById() {
		// Given
		final Long setId = 1L;

		// When
		this.setService.deleteById(setId);

		// Then
		verify(this.setRepository).deleteById(setId);
	}
}
