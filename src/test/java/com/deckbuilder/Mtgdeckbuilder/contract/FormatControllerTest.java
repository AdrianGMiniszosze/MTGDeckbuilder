package com.deckbuilder.Mtgdeckbuilder.contract;

import com.deckbuilder.Mtgdeckbuilder.application.FormatService;
import com.deckbuilder.Mtgdeckbuilder.contract.mapper.FormatMapper;
import com.deckbuilder.Mtgdeckbuilder.model.Format;
import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
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
        testFormat = Format.builder()
                .id(1L)
                .name("Standard")
                .description("Standard format")
                .minDeckSize(60)
                .maxDeckSize(60)
                .maxSideboardSize(15)
                .bannedCards(Arrays.asList("123", "456"))
                .restrictedCards(Arrays.asList("789"))
                .build();

        testFormatDTO = FormatDTO.builder()
                .id(1)
                .format_name("Standard")
                .deck_size(60)
                .build();
    }

    @Test
    @DisplayName("Should list all formats")
    void shouldListAllFormats() {
        // Given
        Format format2 = testFormat.toBuilder().id(2L).name("Commander").maxDeckSize(100).build();
        FormatDTO formatDTO2 = FormatDTO.builder()
                .id(2)
                .format_name("Commander")
                .deck_size(100)
                .build();

        when(formatService.getAll()).thenReturn(Arrays.asList(testFormat, format2));
        when(formatMapper.toDtoList(anyList())).thenReturn(Arrays.asList(testFormatDTO, formatDTO2));

        // When
        ResponseEntity<List<FormatDTO>> response = formatController.listFormats(null, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getFormat_name()).isEqualTo("Standard");
        assertThat(response.getBody().get(1).getFormat_name()).isEqualTo("Commander");
        verify(formatService).getAll();
    }

    @Test
    @DisplayName("Should get format by ID when it exists")
    void shouldGetFormatById_WhenExists() {
        // Given
        when(formatService.findById(1L)).thenReturn(Optional.of(testFormat));
        when(formatMapper.toDto(testFormat)).thenReturn(testFormatDTO);

        // When
        ResponseEntity<FormatDTO> response = formatController.getFormatById(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(1);
        assertThat(response.getBody().getFormat_name()).isEqualTo("Standard");
        verify(formatService).findById(1L);
    }

    @Test
    @DisplayName("Should return 404 when format not found by ID")
    void shouldReturn404_WhenFormatNotFound() {
        // Given
        when(formatService.findById(999L)).thenReturn(Optional.empty());

        // When
        ResponseEntity<FormatDTO> response = formatController.getFormatById(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(formatService).findById(999L);
    }

    @Test
    @DisplayName("Should create new format and return 201")
    void shouldCreateFormat() {
        // Given
        FormatDTO newFormatDTO = FormatDTO.builder()
                .format_name("Modern")
                .deck_size(60)
                .build();

        Format newFormat = testFormat.toBuilder().id(null).name("Modern").build();
        Format createdFormat = testFormat.toBuilder().id(3L).name("Modern").build();
        
        FormatDTO createdFormatDTO = FormatDTO.builder()
                .id(3)
                .format_name("Modern")
                .deck_size(60)
                .build();

        when(formatMapper.toModel(newFormatDTO)).thenReturn(newFormat);
        when(formatService.create(any(Format.class))).thenReturn(createdFormat);
        when(formatMapper.toDto(createdFormat)).thenReturn(createdFormatDTO);

        // When
        ResponseEntity<FormatDTO> response = formatController.createFormat(newFormatDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(3);
        assertThat(response.getBody().getFormat_name()).isEqualTo("Modern");
        verify(formatService).create(any(Format.class));
    }

    @Test
    @DisplayName("Should update existing format and return 200")
    void shouldUpdateFormat_WhenExists() {
        // Given
        FormatDTO updatedDTO = FormatDTO.builder()
                .id(1)
                .format_name("Standard Updated")
                .deck_size(60)
                .build();

        Format updatedFormat = testFormat.toBuilder().name("Standard Updated").build();
        Format savedFormat = testFormat.toBuilder().name("Standard Updated").build();
        
        FormatDTO savedDTO = FormatDTO.builder()
                .id(1)
                .format_name("Standard Updated")
                .deck_size(60)
                .build();

        when(formatMapper.toModel(updatedDTO)).thenReturn(updatedFormat);
        when(formatService.update(eq(1L), any(Format.class))).thenReturn(savedFormat);
        when(formatMapper.toDto(savedFormat)).thenReturn(savedDTO);

        // When
        ResponseEntity<FormatDTO> response = formatController.updateFormat(1, updatedDTO);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getFormat_name()).isEqualTo("Standard Updated");
        verify(formatService).update(eq(1L), any(Format.class));
    }

    @Test
    @DisplayName("Should delete format and return 204 when successful")
    void shouldDeleteFormat_WhenExists() {
        // Given
        when(formatService.deleteById(1L)).thenReturn(true);

        // When
        ResponseEntity<Void> response = formatController.deleteFormat(1);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(formatService).deleteById(1L);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent format")
    void shouldReturn404_WhenDeletingNonExistentFormat() {
        // Given
        when(formatService.deleteById(999L)).thenReturn(false);

        // When
        ResponseEntity<Void> response = formatController.deleteFormat(999);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(formatService).deleteById(999L);
    }
}
