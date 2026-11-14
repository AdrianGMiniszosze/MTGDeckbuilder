package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.SetEntity;
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
        this.testSetEntity = new SetEntity();
        this.testSetEntity.setId(1L);
        this.testSetEntity.setName("Core Set 2021");
	}

	@Test
	@DisplayName("Should find set by ID when it exists")
	void shouldFindSetById_WhenExists() {
		// Given
		when(this.setRepository.findById(1L)).thenReturn(Optional.of(this.testSetEntity));

		// When
		final Optional<SetEntity> result = this.setRepository.findById(1L);

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
		final Optional<SetEntity> result = this.setRepository.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.setRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find set by name when it exists")
	void shouldFindSetByName_WhenExists() {
		// Given
		when(this.setRepository.findByName("Core Set 2021")).thenReturn(Optional.of(this.testSetEntity));

		// When
		final Optional<SetEntity> result = this.setRepository.findByName("Core Set 2021");

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
		final Optional<SetEntity> result = this.setRepository.findByName("NonExistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.setRepository).findByName("NonExistent");
	}

	@Test
	@DisplayName("Should find all sets with pagination")
	void shouldFindAllSetsWithPagination() {
		// Given
		final SetEntity set2 = new SetEntity();
		set2.setId(2L);
		set2.setName("Zendikar Rising");

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<SetEntity> page = new PageImpl<>(Arrays.asList(this.testSetEntity, set2));
		when(this.setRepository.findAll(pageable)).thenReturn(page);

		// When
		final Page<SetEntity> result = this.setRepository.findAll(pageable);

		// Then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0).getName()).isEqualTo("Core Set 2021");
		assertThat(result.getContent().get(1).getName()).isEqualTo("Zendikar Rising");
		verify(this.setRepository).findAll(pageable);
	}

	@Test
	@DisplayName("Should save set entity")
	void shouldSaveSet() {
		// Given
		when(this.setRepository.save(any(SetEntity.class))).thenReturn(this.testSetEntity);

		// When
		final SetEntity result = this.setRepository.save(this.testSetEntity);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Core Set 2021");
		verify(this.setRepository).save(this.testSetEntity);
	}

	@Test
	@DisplayName("Should delete set by ID")
	void shouldDeleteSetById() {
		// Given
		final Long setId = 1L;

		// When
        this.setRepository.deleteById(setId);

		// Then
		verify(this.setRepository).deleteById(setId);
	}

	@Test
	@DisplayName("Should check if set exists by ID")
	void shouldCheckIfSetExists() {
		// Given
		when(this.setRepository.existsById(1L)).thenReturn(true);
		when(this.setRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean exists = this.setRepository.existsById(1L);
		final boolean notExists = this.setRepository.existsById(999L);

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.setRepository).existsById(1L);
		verify(this.setRepository).existsById(999L);
	}
}
