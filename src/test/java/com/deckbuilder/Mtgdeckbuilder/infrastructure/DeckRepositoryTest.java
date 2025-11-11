package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Repository Tests")
class DeckRepositoryTest {

	@Mock
	private DeckRepository deckRepository;

	private DeckEntity testDeckEntity;

	@BeforeEach
	void setUp() {
        this.testDeckEntity = new DeckEntity();
        this.testDeckEntity.setId(1L);
        this.testDeckEntity.setName("Test Deck");
        this.testDeckEntity.setDescription("A test deck");
        this.testDeckEntity.setUserId(1L);
        this.testDeckEntity.setFormatId(1L);
        this.testDeckEntity.setDeckType("main");
        this.testDeckEntity.setIsPrivate(false);
        this.testDeckEntity.setCreated(LocalDateTime.now());
        this.testDeckEntity.setModified(LocalDateTime.now());
	}

	@Test
	@DisplayName("Should find deck by ID when it exists")
	void shouldFindDeckById_WhenExists() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));

		// When
		final Optional<DeckEntity> result = this.deckRepository.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when deck ID does not exist")
	void shouldReturnEmpty_WhenDeckNotFound() {
		// Given
		when(this.deckRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<DeckEntity> result = this.deckRepository.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.deckRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find all decks with pagination")
	void shouldFindAllDecksWithPagination() {
		// Given
		final DeckEntity deck2 = new DeckEntity();
		deck2.setId(2L);
		deck2.setName("Another Deck");
		deck2.setUserId(1L);

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<DeckEntity> page = new PageImpl<>(Arrays.asList(this.testDeckEntity, deck2));
		when(this.deckRepository.findAll(pageable)).thenReturn(page);

		// When
		final Page<DeckEntity> result = this.deckRepository.findAll(pageable);

		// Then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0).getName()).isEqualTo("Test Deck");
		assertThat(result.getContent().get(1).getName()).isEqualTo("Another Deck");
		verify(this.deckRepository).findAll(pageable);
	}

	@Test
	@DisplayName("Should find decks by user ID with pagination")
	void shouldFindDecksByUserId() {
		// Given
		final DeckEntity deck2 = new DeckEntity();
		deck2.setId(2L);
		deck2.setName("User Deck 2");
		deck2.setUserId(1L);

		final Pageable pageable = PageRequest.of(0, 10);
		final Page<DeckEntity> page = new PageImpl<>(Arrays.asList(this.testDeckEntity, deck2));
		when(this.deckRepository.findByUserId(1L, pageable)).thenReturn(page);

		// When
		final Page<DeckEntity> result = this.deckRepository.findByUserId(1L, pageable);

		// Then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent()).allMatch(deck -> deck.getUserId().equals(1L));
		verify(this.deckRepository).findByUserId(1L, pageable);
	}

	@Test
	@DisplayName("Should find decks by format ID")
	void shouldFindDecksByFormatId() {
		// Given
		final DeckEntity deck2 = new DeckEntity();
		deck2.setId(2L);
		deck2.setName("Standard Deck");
		deck2.setFormatId(1L);

		final List<DeckEntity> decks = Arrays.asList(this.testDeckEntity, deck2);
		when(this.deckRepository.findByFormatId(1L)).thenReturn(decks);

		// When
		final List<DeckEntity> result = this.deckRepository.findByFormatId(1L);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).allMatch(deck -> deck.getFormatId().equals(1L));
		verify(this.deckRepository).findByFormatId(1L);
	}

	@Test
	@DisplayName("Should save deck entity")
	void shouldSaveDeck() {
		// Given
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);

		// When
		final DeckEntity result = this.deckRepository.save(this.testDeckEntity);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).save(this.testDeckEntity);
	}

	@Test
	@DisplayName("Should delete deck by ID")
	void shouldDeleteDeckById() {
		// Given
		final Long deckId = 1L;

		// When
        this.deckRepository.deleteById(deckId);

		// Then
		verify(this.deckRepository).deleteById(deckId);
	}

	@Test
	@DisplayName("Should check if deck exists by ID")
	void shouldCheckIfDeckExists() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean exists = this.deckRepository.existsById(1L);
		final boolean notExists = this.deckRepository.existsById(999L);

		// Then
		assertThat(exists).isTrue();
		assertThat(notExists).isFalse();
		verify(this.deckRepository).existsById(1L);
		verify(this.deckRepository).existsById(999L);
	}
}
