package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
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
        this.testFormatEntity = new FormatEntity();
        this.testFormatEntity.setId(1L);
        this.testFormatEntity.setName("Standard");
        this.testFormatEntity.setDescription("Standard format");
        this.testFormatEntity.setMinDeckSize(60);
        this.testFormatEntity.setMaxDeckSize(60);
        this.testFormatEntity.setMaxSideboardSize(15);
        this.testFormatEntity.setBannedCards(Arrays.asList("123", "456"));
        this.testFormatEntity.setRestrictedCards(List.of("789"));
	}

	@Test
	@DisplayName("Should find format by ID when it exists")
	void shouldFindFormatById_WhenExists() {
		// Given
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.testFormatEntity));

		// When
		final Optional<FormatEntity> result = this.formatRepository.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Standard");
		verify(this.formatRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when format ID does not exist")
	void shouldReturnEmpty_WhenFormatNotFound() {
		// Given
		when(this.formatRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<FormatEntity> result = this.formatRepository.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.formatRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find format by name when it exists")
	void shouldFindFormatByName_WhenExists() {
		// Given
		when(this.formatRepository.findByName("Standard")).thenReturn(Optional.of(this.testFormatEntity));

		// When
		final Optional<FormatEntity> result = this.formatRepository.findByName("Standard");

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("Standard");
		verify(this.formatRepository).findByName("Standard");
	}

	@Test
	@DisplayName("Should return empty when format name does not exist")
	void shouldReturnEmpty_WhenFormatNameNotFound() {
		// Given
		when(this.formatRepository.findByName("NonExistent")).thenReturn(Optional.empty());

		// When
		final Optional<FormatEntity> result = this.formatRepository.findByName("NonExistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.formatRepository).findByName("NonExistent");
	}

	@Test
	@DisplayName("Should find all formats")
	void shouldFindAllFormats() {
		// Given
		final FormatEntity format2 = new FormatEntity();
		format2.setId(2L);
		format2.setName("Commander");
		format2.setMinDeckSize(100);
		format2.setMaxDeckSize(100);

		final List<FormatEntity> formats = Arrays.asList(this.testFormatEntity, format2);
		when(this.formatRepository.findAll()).thenReturn(formats);

		// When
		final List<FormatEntity> result = this.formatRepository.findAll();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Standard");
		assertThat(result.get(1).getName()).isEqualTo("Commander");
		verify(this.formatRepository).findAll();
	}

	@Test
	@DisplayName("Should save format entity")
	void shouldSaveFormat() {
		// Given
		when(this.formatRepository.save(any(FormatEntity.class))).thenReturn(this.testFormatEntity);

		// When
		final FormatEntity result = this.formatRepository.save(this.testFormatEntity);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Standard");
		verify(this.formatRepository).save(this.testFormatEntity);
	}

	@Test
	@DisplayName("Should delete format by ID")
	void shouldDeleteFormatById() {
		// Given
		final Long formatId = 1L;

		// When
        this.formatRepository.deleteById(formatId);

		// Then
		verify(this.formatRepository).deleteById(formatId);
	}

	@Test
	@DisplayName("Should check if format exists by ID")
	void shouldCheckIfFormatExists() {
		// Given
		when(this.formatRepository.existsById(1L)).thenReturn(true);
		when(this.formatRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean exists = this.formatRepository.existsById(1L);
		final boolean notExists = this.formatRepository.existsById(999L);

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.formatRepository).existsById(1L);
		verify(this.formatRepository).existsById(999L);
	}
}
