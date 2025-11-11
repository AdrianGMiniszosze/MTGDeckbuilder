package com.deckbuilder.mtgdeckbuilder.application.implement;

import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.FormatRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.FormatEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.Format;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Format Service Implementation Tests")
class FormatServiceImplTest {

	@Mock
	private FormatRepository formatRepository;

	@Mock
	private FormatEntityMapper formatEntityMapper;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private CardInDeckRepository cardInDeckRepository;

	@Mock
	private CardEntityMapper cardEntityMapper;

	@InjectMocks
	private FormatServiceImpl formatService;

	private Format testFormat;
	private FormatEntity testFormatEntity;

	@BeforeEach
	void setUp() {
        this.testFormat = Format.builder().id(1L).name("Standard").description("Standard format").minDeckSize(60)
				.maxDeckSize(60).maxSideboardSize(15).bannedCards(Arrays.asList("123", "456"))
				.restrictedCards(List.of("789")).build();

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
	@DisplayName("Should get all formats")
	void shouldGetAllFormats() {
		// Given
		final Format format2 = this.testFormat.toBuilder().id(2L).name("Commander").build();
		final FormatEntity entity2 = new FormatEntity();
		entity2.setId(2L);
		entity2.setName("Commander");

		when(this.formatRepository.findAll()).thenReturn(Arrays.asList(this.testFormatEntity, entity2));
		when(this.formatEntityMapper.toModelList(anyList())).thenReturn(Arrays.asList(this.testFormat, format2));

		// When
		final List<Format> result = this.formatService.getAll();

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Standard");
		assertThat(result.get(1).getName()).isEqualTo("Commander");
		verify(this.formatRepository).findAll();
	}

	@Test
	@DisplayName("Should find format by ID when it exists")
	void shouldFindFormatById_WhenExists() {
		// Given
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.testFormatEntity));
		when(this.formatEntityMapper.toModel(this.testFormatEntity)).thenReturn(this.testFormat);

		// When
		final Optional<Format> result = this.formatService.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Standard");
		verify(this.formatRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when format not found by ID")
	void shouldReturnEmpty_WhenFormatNotFoundById() {
		// Given
		when(this.formatRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Format> result = this.formatService.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.formatRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find format by name when it exists")
	void shouldFindFormatByName_WhenExists() {
		// Given
		when(this.formatRepository.findByName("Standard")).thenReturn(Optional.of(this.testFormatEntity));
		when(this.formatEntityMapper.toModel(this.testFormatEntity)).thenReturn(this.testFormat);

		// When
		final Optional<Format> result = this.formatService.findByName("Standard");

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("Standard");
		verify(this.formatRepository).findByName("Standard");
	}

	@Test
	@DisplayName("Should return empty when format not found by name")
	void shouldReturnEmpty_WhenFormatNotFoundByName() {
		// Given
		when(this.formatRepository.findByName("NonExistent")).thenReturn(Optional.empty());

		// When
		final Optional<Format> result = this.formatService.findByName("NonExistent");

		// Then
		assertThat(result).isEmpty();
		verify(this.formatRepository).findByName("NonExistent");
	}

	@Test
	@DisplayName("Should create new format")
	void shouldCreateFormat() {
		// Given
		final Format newFormat = this.testFormat.toBuilder().id(null).build();
		when(this.formatEntityMapper.toEntity(any(Format.class))).thenReturn(this.testFormatEntity);
		when(this.formatRepository.save(any(FormatEntity.class))).thenReturn(this.testFormatEntity);
		when(this.formatEntityMapper.toModel(this.testFormatEntity)).thenReturn(this.testFormat);

		// When
		final Format result = this.formatService.create(newFormat);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Standard");
		verify(this.formatRepository).save(any(FormatEntity.class));
	}

	@Test
	@DisplayName("Should update existing format")
	void shouldUpdateFormat_WhenExists() {
		// Given
		final Format updatedFormat = this.testFormat.toBuilder().description("Updated description").build();
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.testFormatEntity));
		when(this.formatEntityMapper.toEntity(any(Format.class))).thenReturn(this.testFormatEntity);
		when(this.formatRepository.save(any(FormatEntity.class))).thenReturn(this.testFormatEntity);
		when(this.formatEntityMapper.toModel(this.testFormatEntity)).thenReturn(updatedFormat);

		// When
		final Format result = this.formatService.update(1L, updatedFormat);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo("Updated description");
		verify(this.formatRepository).findById(1L);
		verify(this.formatRepository).save(any(FormatEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent format")
	void shouldThrowException_WhenUpdatingNonExistentFormat() {
		// Given
		when(this.formatRepository.findById(999L)).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.formatService.update(999L, this.testFormat)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Format not found with id: 999");

		verify(this.formatRepository).findById(999L);
		verify(this.formatRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should delete format by ID when it exists")
	void shouldDeleteFormat_WhenExists() {
		// Given
		when(this.formatRepository.existsById(1L)).thenReturn(true);
		doNothing().when(this.formatRepository).deleteById(1L);

		// When
		final boolean result = this.formatService.deleteById(1L);

		// Then
		assertThat(result).isTrue();
		verify(this.formatRepository).existsById(1L);
		verify(this.formatRepository).deleteById(1L);
	}

	@Test
	@DisplayName("Should return false when deleting non-existent format")
	void shouldReturnFalse_WhenDeletingNonExistentFormat() {
		// Given
		when(this.formatRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean result = this.formatService.deleteById(999L);

		// Then
		assertThat(result).isFalse();
		verify(this.formatRepository).existsById(999L);
		verify(this.formatRepository, never()).deleteById(any());
	}

	@Test
	@DisplayName("Should return true when card is legal in format")
	void shouldReturnTrue_WhenCardIsLegal() {
		// Given
		final Long cardId = 100L;
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.testFormatEntity));

		// When
		final boolean result = this.formatService.isCardLegal(cardId, 1L);

		// Then
		assertThat(result).isTrue();
		verify(this.formatRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return false when card is banned in format")
	void shouldReturnFalse_WhenCardIsBanned() {
		// Given
		final Long cardId = 123L; // This is in the banned list
		when(this.formatRepository.findById(1L)).thenReturn(Optional.of(this.testFormatEntity));

		// When
		final boolean result = this.formatService.isCardLegal(cardId, 1L);

		// Then
		assertThat(result).isFalse();
		verify(this.formatRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return false when format not found for card legality check")
	void shouldReturnFalse_WhenFormatNotFoundForCardLegality() {
		// Given
		when(this.formatRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final boolean result = this.formatService.isCardLegal(100L, 999L);

		// Then
		assertThat(result).isFalse();
		verify(this.formatRepository).findById(999L);
	}

	@Test
	@DisplayName("Should return true when deck is legal in format")
	void shouldReturnTrue_WhenDeckIsLegal() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		final CardInDeckEntity mainCard1 = CardInDeckEntity.builder().cardId(100L).deckId(deckId).quantity(4).section("main")
				.build();

		final CardInDeckEntity mainCard2 = CardInDeckEntity.builder().cardId(101L).deckId(deckId).quantity(20).section("main")
				.build();

		final CardInDeckEntity sideboardCard = CardInDeckEntity.builder().cardId(102L).deckId(deckId).quantity(10)
				.section("sideboard").build();

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Arrays.asList(mainCard1, mainCard2, sideboardCard));
		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));

		// When (24 cards in main, 10 in sideboard - all legal)
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isFalse(); // False because 24 < 60 (minDeckSize)
		verify(this.formatRepository).existsById(formatId);
		verify(this.cardInDeckRepository).findByDeckId(deckId);
	}

	@Test
	@DisplayName("Should return false when deck is too small")
	void shouldReturnFalse_WhenDeckIsTooSmall() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		final CardInDeckEntity mainCard = CardInDeckEntity.builder().cardId(100L).deckId(deckId).quantity(30).section("main")
				.build();

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Collections.singletonList(mainCard));
		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));

		// When (30 cards < 60 minDeckSize)
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should return false when deck is too large")
	void shouldReturnFalse_WhenDeckIsTooLarge() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		final CardInDeckEntity mainCard = CardInDeckEntity.builder().cardId(100L).deckId(deckId).quantity(70).section("main")
				.build();

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Collections.singletonList(mainCard));
		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));

		// When (70 cards > 60 maxDeckSize)
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should return false when sideboard is too large")
	void shouldReturnFalse_WhenSideboardIsTooLarge() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		final CardInDeckEntity mainCard = CardInDeckEntity.builder().cardId(100L).deckId(deckId).quantity(60).section("main")
				.build();

		final CardInDeckEntity sideboardCard = CardInDeckEntity.builder().cardId(101L).deckId(deckId).quantity(20)
				.section("sideboard").build();

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Arrays.asList(mainCard, sideboardCard));
		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));

		// When (20 cards in sideboard > 15 maxSideboardSize)
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should return false when deck contains banned card")
	void shouldReturnFalse_WhenDeckContainsBannedCard() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		final CardInDeckEntity bannedCard = CardInDeckEntity.builder().cardId(123L) // Banned card from test setup
				.deckId(deckId).quantity(4).section("main").build();

		final CardInDeckEntity legalCard = CardInDeckEntity.builder().cardId(100L).deckId(deckId).quantity(56).section("main")
				.build();

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Arrays.asList(bannedCard, legalCard));
		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));

		// When
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should return false when deck is empty")
	void shouldReturnFalse_WhenDeckIsEmpty() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Collections.emptyList());

		// When
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isFalse();
	}

	@Test
	@DisplayName("Should ignore maybeboard cards when checking deck legality")
	void shouldIgnoreMaybeboardCards() {
		// Given
		final Long deckId = 1L;
		final Long formatId = 1L;

		final CardInDeckEntity mainCard = CardInDeckEntity.builder().cardId(100L).deckId(deckId).quantity(60).section("main")
				.build();

		final CardInDeckEntity maybeboardCard = CardInDeckEntity.builder().cardId(123L) // Banned card but in maybeboard
				.deckId(deckId).quantity(4).section("maybeboard").build();

		when(this.formatRepository.existsById(formatId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckId(deckId)).thenReturn(Arrays.asList(mainCard, maybeboardCard));
		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));

		// When
		final boolean result = this.formatService.isDeckLegal(deckId, formatId);

		// Then
		assertThat(result).isTrue(); // Legal because banned card is in maybeboard
	}

	@Test
	@DisplayName("Should return false for deck legality when format not found")
	void shouldReturnFalse_WhenFormatNotFoundForDeckLegality() {
		// Given
		when(this.formatRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean result = this.formatService.isDeckLegal(1L, 999L);

		// Then
		assertThat(result).isFalse();
		verify(this.formatRepository).existsById(999L);
	}

	@Test
	@DisplayName("Should return cards for valid format ID with pagination")
	void shouldReturnCards_WhenFormatExists() {
		// Given
		final Long formatId = 1L;
		final int pageSize = 10;
		final int pageNumber = 0;

		final CardEntity cardEntity1 = new CardEntity();
		cardEntity1.setId(1L);
		cardEntity1.setName("Card One");

		final CardEntity cardEntity2 = new CardEntity();
		cardEntity2.setId(2L);
		cardEntity2.setName("Card Two");

		final List<CardEntity> cardEntities = Arrays.asList(cardEntity1, cardEntity2);
		final Page<CardEntity> cardPage = new PageImpl<>(cardEntities, PageRequest.of(pageNumber, pageSize),
				cardEntities.size());

		final Card card1 = Card.builder().id(1L).name("Card One").build();
		final Card card2 = Card.builder().id(2L).name("Card Two").build();
		final List<Card> expectedCards = Arrays.asList(card1, card2);

		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));
		when(this.cardRepository.findByFormatId(eq(formatId), any(PageRequest.class))).thenReturn(cardPage);
		when(this.cardEntityMapper.toModelList(cardEntities)).thenReturn(expectedCards);

		// When
		final List<Card> result = this.formatService.findCardsByFormatId(formatId, pageSize, pageNumber);

		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getId()).isEqualTo(1L);
		assertThat(result.get(1).getId()).isEqualTo(2L);
		verify(this.formatRepository).findById(formatId);
		verify(this.cardRepository).findByFormatId(eq(formatId), any(PageRequest.class));
		verify(this.cardEntityMapper).toModelList(cardEntities);
	}

	@Test
	@DisplayName("Should return empty list when no cards are legal in format")
	void shouldReturnEmptyList_WhenNoCardsAreLegal() {
		// Given
		final Long formatId = 1L;
		final Page<CardEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

		when(this.formatRepository.findById(formatId)).thenReturn(Optional.of(this.testFormatEntity));
		when(this.cardRepository.findByFormatId(eq(formatId), any(PageRequest.class))).thenReturn(emptyPage);
		when(this.cardEntityMapper.toModelList(Collections.emptyList())).thenReturn(Collections.emptyList());

		// When
		final List<Card> result = this.formatService.findCardsByFormatId(formatId, 10, 0);

		// Then
		assertThat(result).isNotNull();
		assertThat(result).isEmpty();
		verify(this.formatRepository).findById(formatId);
		verify(this.cardRepository).findByFormatId(eq(formatId), any(PageRequest.class));
	}

	@Test
	@DisplayName("Should throw exception when finding cards for non-existent format")
	void shouldThrowException_WhenFindingCardsForNonExistentFormat() {
		// Given
		when(this.formatRepository.findById(999L)).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.formatService.findCardsByFormatId(999L, 10, 0))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Format not found with id: 999");

		verify(this.formatRepository).findById(999L);
		verifyNoInteractions(this.cardRepository, this.cardEntityMapper);
	}
}
