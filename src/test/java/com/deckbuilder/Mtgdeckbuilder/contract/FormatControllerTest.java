package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.mtgdeckbuilder.application.FormatService;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.FormatMapper;
import com.deckbuilder.mtgdeckbuilder.model.Format;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Format Controller Tests")
class FormatControllerTest {

	@Mock
	private FormatService formatService;

	@Mock
	private FormatMapper formatMapper;

	@InjectMocks
	private FormatController formatController;

	private Format testFormat;
	private FormatDTO testFormatDTO;

	@BeforeEach
	void setUp() {
        this.testFormat = Format.builder().id(1L).name("Standard").description("Standard format").minDeckSize(60)
				.maxDeckSize(60).maxSideboardSize(15).bannedCards(Arrays.asList("123", "456"))
				.restrictedCards(List.of("789")).build();

        this.testFormatDTO = FormatDTO.builder().id(1).format_name("Standard").deck_size(60).build();
	}

	@Test
	@DisplayName("Should list all formats")
	void shouldListAllFormats() {
		// Given
		final Format format2 = this.testFormat.toBuilder().id(2L).name("Commander").maxDeckSize(100).build();
		final FormatDTO formatDTO2 = FormatDTO.builder().id(2).format_name("Commander").deck_size(100).build();

		when(this.formatService.getAll()).thenReturn(Arrays.asList(this.testFormat, format2));
		when(this.formatMapper.toDtoList(anyList())).thenReturn(Arrays.asList(this.testFormatDTO, formatDTO2));

		// When
		final ResponseEntity<List<FormatDTO>> response = this.formatController.listFormats(null, null);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).hasSize(2);
		assertThat(response.getBody().get(0).getFormat_name()).isEqualTo("Standard");
		assertThat(response.getBody().get(1).getFormat_name()).isEqualTo("Commander");
		verify(this.formatService).getAll();
	}

	@Test
	@DisplayName("Should get format by ID when it exists")
	void shouldGetFormatById_WhenExists() {
		// Given
		when(this.formatService.findById(1L)).thenReturn(Optional.of(this.testFormat));
		when(this.formatMapper.toDto(this.testFormat)).thenReturn(this.testFormatDTO);

		// When
		final ResponseEntity<FormatDTO> response = this.formatController.getFormatById(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(1);
		assertThat(response.getBody().getFormat_name()).isEqualTo("Standard");
		verify(this.formatService).findById(1L);
	}

	@Test
	@DisplayName("Should return 404 when format not found by ID")
	void shouldReturn404_WhenFormatNotFound() {
		// Given
		when(this.formatService.findById(999L)).thenReturn(Optional.empty());

		// When
		final ResponseEntity<FormatDTO> response = this.formatController.getFormatById(999);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
		verify(this.formatService).findById(999L);
	}

	@Test
	@DisplayName("Should create new format and return 201")
	void shouldCreateFormat() {
		// Given
		final FormatDTO newFormatDTO = FormatDTO.builder().format_name("Modern").deck_size(60).build();

		final Format newFormat = this.testFormat.toBuilder().id(null).name("Modern").build();
		final Format createdFormat = this.testFormat.toBuilder().id(3L).name("Modern").build();

		final FormatDTO createdFormatDTO = FormatDTO.builder().id(3).format_name("Modern").deck_size(60).build();

		when(this.formatMapper.toModel(newFormatDTO)).thenReturn(newFormat);
		when(this.formatService.create(any(Format.class))).thenReturn(createdFormat);
		when(this.formatMapper.toDto(createdFormat)).thenReturn(createdFormatDTO);

		// When
		final ResponseEntity<FormatDTO> response = this.formatController.createFormat(newFormatDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(3);
		assertThat(response.getBody().getFormat_name()).isEqualTo("Modern");
		verify(this.formatService).create(any(Format.class));
	}

	@Test
	@DisplayName("Should update existing format and return 200")
	void shouldUpdateFormat_WhenExists() {
		// Given
		final FormatDTO updatedDTO = FormatDTO.builder().id(1).format_name("Standard Updated").deck_size(60).build();

		final Format updatedFormat = this.testFormat.toBuilder().name("Standard Updated").build();
		final Format savedFormat = this.testFormat.toBuilder().name("Standard Updated").build();

		final FormatDTO savedDTO = FormatDTO.builder().id(1).format_name("Standard Updated").deck_size(60).build();

		when(this.formatMapper.toModel(updatedDTO)).thenReturn(updatedFormat);
		when(this.formatService.update(eq(1L), any(Format.class))).thenReturn(savedFormat);
		when(this.formatMapper.toDto(savedFormat)).thenReturn(savedDTO);

		// When
		final ResponseEntity<FormatDTO> response = this.formatController.updateFormat(1, updatedDTO);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getFormat_name()).isEqualTo("Standard Updated");
		verify(this.formatService).update(eq(1L), any(Format.class));
	}

	@Test
	@DisplayName("Should delete format and return 204 when successful")
	void shouldDeleteFormat_WhenExists() {
		// Given
		when(this.formatService.deleteById(1L)).thenReturn(true);

		// When
		final ResponseEntity<Void> response = this.formatController.deleteFormat(1);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(response.getBody()).isNull();
		verify(this.formatService).deleteById(1L);
	}

	@Test
	@DisplayName("Should return 404 when deleting non-existent format")
	void shouldReturn404_WhenDeletingNonExistentFormat() {
		// Given
		when(this.formatService.deleteById(999L)).thenReturn(false);

		// When
		final ResponseEntity<Void> response = this.formatController.deleteFormat(999);

		// Then
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(response.getBody()).isNull();
		verify(this.formatService).deleteById(999L);
	}
}
