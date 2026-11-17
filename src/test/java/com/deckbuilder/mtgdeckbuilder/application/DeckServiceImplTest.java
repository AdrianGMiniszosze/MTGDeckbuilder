package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.DeckServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardInDeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckCommanderRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.DeckRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.CardNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotLegalException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.DeckNotFoundException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.exception.InvalidDeckCompositionException;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.DeckEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardInDeckEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckCommanderEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.DeckEntity;
import com.deckbuilder.mtgdeckbuilder.model.Deck;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckStatus;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Deck Service Implementation Tests")
class DeckServiceImplTest {

	@Mock
	private DeckRepository deckRepository;

	@Mock
	private DeckEntityMapper deckEntityMapper;

	@Mock
	private CardInDeckRepository cardInDeckRepository;

	@Mock
	private CardRepository cardRepository;

	@Mock
	private DeckValidationService deckValidationService;

	@Mock
	private DeckCommanderRepository deckCommanderRepository;

	@InjectMocks
	private DeckServiceImpl deckService;

	private Deck testDeck;
	private DeckEntity testDeckEntity;
	private CardInDeckEntity testCardInDeck;

	@BeforeEach
	void setUp() {
		this.testDeck = Deck.builder().id(1L).name("Test Deck").description("A test deck").userId(1L).formatId(1L)
				.isPrivate(false).created(LocalDateTime.now()).modified(LocalDateTime.now()).build();

		this.testDeckEntity = new DeckEntity();
		this.testDeckEntity.setId(1L);
		this.testDeckEntity.setName("Test Deck");
		this.testDeckEntity.setDescription("A test deck");
		this.testDeckEntity.setUserId(1L);
		this.testDeckEntity.setFormatId(1L);
		this.testDeckEntity.setIsPrivate(false);
		this.testDeckEntity.setCreated(LocalDateTime.now());
		this.testDeckEntity.setModified(LocalDateTime.now());

		this.testCardInDeck = CardInDeckEntity.builder().id(1L).deckId(1L).cardId(100L).quantity(2).section("main")
				.build();
	}

	@Test
	@DisplayName("Should get all decks with pagination")
	void shouldGetAllDecks() {
		// Given
		final Deck deck2 = this.testDeck.toBuilder().id(2L).name("Deck 2").build();
		final DeckEntity entity2 = new DeckEntity();
		entity2.setId(2L);
		entity2.setName("Deck 2");

		final Page<DeckEntity> page = new PageImpl<>(Arrays.asList(this.testDeckEntity, entity2));
		when(this.deckRepository.findAll(any(Pageable.class))).thenReturn(page);
		when(this.cardInDeckRepository.findByDeckIdIn(anyCollection())).thenReturn(List.of(this.testCardInDeck));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);
		when(this.deckEntityMapper.toModel(entity2)).thenReturn(deck2);

		// When
		final List<Deck> result = this.deckService.getAll(10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should attach the card rows of every listed deck in one query")
	void shouldAttachCardsToListedDecks() {
		// Given
		final DeckEntity entity2 = new DeckEntity();
		entity2.setId(2L);
		entity2.setName("Deck 2");
		final CardInDeckEntity cardOfDeck2 = CardInDeckEntity.builder().id(2L).deckId(2L).cardId(200L).quantity(1)
				.section("main").build();
		final Deck deck2 = this.testDeck.toBuilder().id(2L).name("Deck 2").build();

		when(this.deckRepository.findAll(any(Pageable.class)))
				.thenReturn(new PageImpl<>(Arrays.asList(this.testDeckEntity, entity2)));
		when(this.cardInDeckRepository.findByDeckIdIn(anyCollection()))
				.thenReturn(Arrays.asList(this.testCardInDeck, cardOfDeck2));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);
		when(this.deckEntityMapper.toModel(entity2)).thenReturn(deck2);

		// When
		final List<Deck> result = this.deckService.getAll(10, 0);

		// Then
		assertThat(result.get(0).getCards()).extracting(Deck.CardInDeck::getCardId).containsExactly(100L);
		assertThat(result.get(1).getCards()).extracting(Deck.CardInDeck::getCardId).containsExactly(200L);
		verify(this.cardInDeckRepository).findByDeckIdIn(anyCollection());
		verify(this.cardInDeckRepository, never()).findByDeckId(anyLong());
	}

	@Test
	@DisplayName("Should not query cards when there are no decks")
	void shouldNotQueryCards_WhenNoDecks() {
		// Given
		when(this.deckRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

		// When
		final List<Deck> result = this.deckService.getAll(10, 0);

		// Then
		assertThat(result).isEmpty();
		verify(this.cardInDeckRepository, never()).findByDeckIdIn(anyCollection());
	}

	@Test
	@DisplayName("Should find deck by ID when it exists")
	void shouldFindDeckById_WhenExists() {
		// Given
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Optional<Deck> result = this.deckService.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).findById(1L);
	}

	@Test
	@DisplayName("Should return empty when deck not found by ID")
	void shouldReturnEmpty_WhenDeckNotFound() {
		// Given
		when(this.deckRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Deck> result = this.deckService.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.deckRepository).findById(999L);
	}

	@Test
	@DisplayName("Should find decks by user ID with pagination")
	void shouldFindDecksByUserId() {
		// Given
		final Page<DeckEntity> page = new PageImpl<>(Collections.singletonList(this.testDeckEntity));
		when(this.deckRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(page);
		when(this.cardInDeckRepository.findByDeckIdIn(anyCollection())).thenReturn(List.of(this.testCardInDeck));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final List<Deck> result = this.deckService.findByUserId(1L, 10, 0);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getUserId()).isEqualTo(1L);
		assertThat(result.get(0).getCards()).hasSize(1);
		verify(this.deckRepository).findByUserId(eq(1L), any(Pageable.class));
	}

	@Test
	@DisplayName("Should find decks by format ID")
	void shouldFindDecksByFormat() {
		// Given
		when(this.deckRepository.findByFormatId(1L)).thenReturn(Collections.singletonList(this.testDeckEntity));
		when(this.cardInDeckRepository.findByDeckIdIn(anyCollection())).thenReturn(List.of(this.testCardInDeck));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final List<Deck> result = this.deckService.findByFormat(1L);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getFormatId()).isEqualTo(1L);
		assertThat(result.get(0).getCards()).hasSize(1);
		verify(this.deckRepository).findByFormatId(1L);
	}

	@Test
	@DisplayName("Should create new deck")
	void shouldCreateDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.create(newDeck);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Test Deck");
		verify(this.deckRepository).save(any(DeckEntity.class));
	}

	@Test
	@DisplayName("Should persist deck contents when creating a deck")
	void shouldPersistDeckContents_WhenCreatingDeck() {
		// Given
		final Deck.CardInDeck mainCard = new Deck.CardInDeck(null, 10L, null, 2, "main");
		final Deck.CardInDeck sideCard = new Deck.CardInDeck(null, 10L, null, 1, "sideboard");
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.cards(List.of(mainCard, sideCard)).build();

		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.create(newDeck);

		// Then
		verify(this.cardInDeckRepository).deleteByDeckId(1L);
		final ArgumentCaptor<CardInDeckEntity> captor = ArgumentCaptor.forClass(CardInDeckEntity.class);
		verify(this.cardInDeckRepository, times(2)).save(captor.capture());
		assertThat(captor.getAllValues()).allMatch(card -> card.getDeckId().equals(1L));
		assertThat(captor.getAllValues()).extracting(CardInDeckEntity::getSection)
				.containsExactlyInAnyOrder("main", "sideboard");
	}

	@Test
	@DisplayName("Should merge duplicate cards of the same section when creating a deck")
	void shouldMergeDuplicateCards_WhenCreatingDeck() {
		// Given
		final Deck.CardInDeck first = new Deck.CardInDeck(null, 10L, null, 2, "main");
		final Deck.CardInDeck duplicate = new Deck.CardInDeck(null, 10L, null, 3, null);
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.cards(List.of(first, duplicate)).build();

		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.create(newDeck);

		// Then
		final ArgumentCaptor<CardInDeckEntity> captor = ArgumentCaptor.forClass(CardInDeckEntity.class);
		verify(this.cardInDeckRepository).save(captor.capture());
		assertThat(captor.getValue().getSection()).isEqualTo("main");
		assertThat(captor.getValue().getQuantity()).isEqualTo(5);
	}

	@Test
	@DisplayName("Should persist commanders when creating a deck")
	void shouldPersistCommanders_WhenCreatingDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.commanderCardIds(List.of(100L, 200L)).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.create(newDeck);

		// Then
		verify(this.deckCommanderRepository).deleteByDeckId(1L);
		verify(this.deckCommanderRepository).save(new DeckCommanderEntity(1L, 100L));
		verify(this.deckCommanderRepository).save(new DeckCommanderEntity(1L, 200L));
	}

	@Test
	@DisplayName("Should replace commanders when updating a deck")
	void shouldReplaceCommanders_WhenUpdatingDeck() {
		// Given
		final Deck update = this.testDeck.toBuilder().id(1L).commanderCardIds(List.of(200L, 300L)).build();
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.update(1L, update);

		// Then
		verify(this.deckCommanderRepository).deleteByDeckId(1L);
		verify(this.deckCommanderRepository).save(new DeckCommanderEntity(1L, 200L));
		verify(this.deckCommanderRepository).save(new DeckCommanderEntity(1L, 300L));
	}

	@Test
	@DisplayName("Should preserve commanders when the update omits them")
	void shouldPreserveCommanders_WhenUpdateOmitsThem() {
		// Given
		final Deck update = this.testDeck.toBuilder().id(1L).commanderCardIds(null).build();
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.update(1L, update);

		// Then
		verify(this.deckCommanderRepository, never()).deleteByDeckId(anyLong());
		verify(this.deckCommanderRepository, never()).save(any(DeckCommanderEntity.class));
	}

	@Test
	@DisplayName("Should reject a null commander id")
	void shouldRejectNullCommanderId() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.commanderCardIds(Collections.singletonList(null)).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);

		// When/Then
		assertThatThrownBy(() -> this.deckService.create(newDeck))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Commander card ids must not be null");
		verify(this.deckCommanderRepository, never()).save(any(DeckCommanderEntity.class));
	}

	@Test
	@DisplayName("Should attach the commanders of every listed deck in one query")
	void shouldAttachCommandersToListedDecks() {
		// Given
		final DeckEntity entity2 = new DeckEntity();
		entity2.setId(2L);
		entity2.setName("Deck 2");
		final Deck deck2 = this.testDeck.toBuilder().id(2L).name("Deck 2").build();

		when(this.deckRepository.findAll(any(Pageable.class)))
				.thenReturn(new PageImpl<>(Arrays.asList(this.testDeckEntity, entity2)));
		when(this.cardInDeckRepository.findByDeckIdIn(anyCollection())).thenReturn(List.of());
		when(this.deckCommanderRepository.findByDeckIdIn(anyCollection()))
				.thenReturn(List.of(new DeckCommanderEntity(1L, 10L), new DeckCommanderEntity(1L, 5L)));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);
		when(this.deckEntityMapper.toModel(entity2)).thenReturn(deck2);

		// When
		final List<Deck> result = this.deckService.getAll(10, 0);

		// Then
		assertThat(result.get(0).getCommanderCardIds()).containsExactly(5L, 10L);
		assertThat(result.get(1).getCommanderCardIds()).isEmpty();
		verify(this.deckCommanderRepository).findByDeckIdIn(anyCollection());
	}

	@Test
	@DisplayName("Should reject an unknown section when creating a deck")
	void shouldRejectUnknownSection_WhenCreatingDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.cards(List.of(new Deck.CardInDeck(null, 10L, null, 1, "graveyard"))).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);

		// When / Then
		assertThatThrownBy(() -> this.deckService.create(newDeck))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Invalid section");
		verify(this.cardInDeckRepository, never()).save(any(CardInDeckEntity.class));
	}

	@Test
	@DisplayName("Should reject a deck row without a card id")
	void shouldRejectMissingCardId_WhenCreatingDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.cards(List.of(new Deck.CardInDeck(null, null, null, 1, "main"))).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);

		// When / Then
		assertThatThrownBy(() -> this.deckService.create(newDeck))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Card id is required");
	}

	@Test
	@DisplayName("Should reject a non-positive quantity when creating a deck")
	void shouldRejectInvalidQuantity_WhenCreatingDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null)
				.cards(List.of(new Deck.CardInDeck(null, 10L, null, 0, "main"))).build();
		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);

		// When / Then
		assertThatThrownBy(() -> this.deckService.create(newDeck))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");
	}

	@Test
	@DisplayName("Should keep existing deck contents when the payload omits them")
	void shouldKeepDeckContents_WhenUpdateOmitsThem() {
		// Given
		final Deck updatedDeck = this.testDeck.toBuilder().name("Updated Deck").cards(null).build();
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(updatedDeck);

		// When
		this.deckService.update(1L, updatedDeck);

		// Then
		verify(this.cardInDeckRepository, never()).deleteByDeckId(anyLong());
	}

	@Test
	@DisplayName("Should update existing deck")
	void shouldUpdateDeck_WhenExists() {
		// Given
		final Deck updatedDeck = this.testDeck.toBuilder().name("Updated Deck").build();
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(updatedDeck);

		// When
		final Deck result = this.deckService.update(1L, updatedDeck);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Updated Deck");
		verify(this.deckRepository).findById(1L);
		verify(this.deckRepository).save(any(DeckEntity.class));
	}

	@Test
	@DisplayName("Should preserve privacy when the update omits it")
	void shouldPreservePrivacy_WhenUpdateOmitsIt() {
		// Given
		this.testDeckEntity.setIsPrivate(false);
		final Deck update = this.testDeck.toBuilder().isPrivate(null).build();
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenReturn(this.testDeckEntity);
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.update(1L, update);

		// Then
		final ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
		verify(this.deckEntityMapper).updateEntity(captor.capture(), any(DeckEntity.class));
		assertThat(captor.getValue().getIsPrivate()).isFalse();
	}

	@Test
	@DisplayName("Should throw exception when updating non-existent deck")
	void shouldThrowException_WhenUpdatingNonExistentDeck() {
		// Given
		when(this.deckRepository.findById(999L)).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.deckService.update(999L, this.testDeck)).isInstanceOf(DeckNotFoundException.class)
				.hasMessageContaining("Deck not found with id: 999");

		verify(this.deckRepository).findById(999L);
		verify(this.deckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should delete deck by ID when it exists")
	void shouldDeleteDeck_WhenExists() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);
		doNothing().when(this.deckRepository).deleteById(1L);

		// When
		final boolean result = this.deckService.deleteById(1L);

		// Then
		assertThat(result).isTrue();
		verify(this.deckRepository).existsById(1L);
		verify(this.deckRepository).deleteById(1L);
	}

	@Test
	@DisplayName("Should return false when deleting non-existent deck")
	void shouldReturnFalse_WhenDeletingNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When
		final boolean result = this.deckService.deleteById(999L);

		// Then
		assertThat(result).isFalse();
		verify(this.deckRepository).existsById(999L);
		verify(this.deckRepository, never()).deleteById(any());
	}

	// ==================== LIFECYCLE TESTS ====================

	@Test
	@DisplayName("Should delegate validation to the validation service")
	void shouldValidateDeck() {
		// Given
		final DeckValidationResult expected = validationResult();
		when(this.deckValidationService.evaluate(1L)).thenReturn(expected);

		// When
		final DeckValidationResult result = this.deckService.validate(1L);

		// Then
		assertThat(result).isSameAs(expected);
		verify(this.deckValidationService).evaluate(1L);
	}

	@Test
	@DisplayName("Should publish a legal deck")
	void shouldPublishLegalDeck() {
		// Given
		when(this.deckValidationService.evaluate(1L)).thenReturn(validationResult());
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.publish(1L);

		// Then
		final ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
		verify(this.deckRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(DeckStatus.PUBLISHED.getValue());
		assertThat(captor.getValue().getPublishedAt()).isNotNull();
	}

	@Test
	@DisplayName("Should publish a deck that only has warnings")
	void shouldPublishDeckWithWarnings() {
		// Given
		final DeckIssue warning = DeckIssue.builder()
				.severity(IssueSeverity.WARNING)
				.code(DeckIssueCode.LEGALITY_UNKNOWN)
				.message("Unknown legality")
				.build();
		when(this.deckValidationService.evaluate(1L)).thenReturn(validationResult(warning));
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.publish(1L);

		// Then
		assertThat(this.testDeckEntity.getStatus()).isEqualTo(DeckStatus.PUBLISHED.getValue());
		verify(this.deckRepository).save(any(DeckEntity.class));
	}

	@Test
	@DisplayName("Should reject publishing a deck with errors")
	void shouldRejectPublishingIllegalDeck() {
		// Given
		final DeckIssue error = DeckIssue.builder()
				.severity(IssueSeverity.ERROR)
				.code(DeckIssueCode.CARD_BANNED)
				.message("Black Lotus is banned")
				.build();
		final DeckValidationResult illegal = validationResult(error);
		when(this.deckValidationService.evaluate(1L)).thenReturn(illegal);

		// When/Then
		assertThatThrownBy(() -> this.deckService.publish(1L))
				.isInstanceOf(DeckNotLegalException.class)
				.hasMessageContaining("1 blocking issue");
		verify(this.deckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should return a published deck to draft")
	void shouldUnpublishDeck() {
		// Given
		this.testDeckEntity.setStatus(DeckStatus.PUBLISHED.getValue());
		this.testDeckEntity.setPublishedAt(LocalDateTime.now());
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.unpublish(1L);

		// Then
		final ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
		verify(this.deckRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(DeckStatus.DRAFT.getValue());
		assertThat(captor.getValue().getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("Should throw when publishing a non-existent deck")
	void shouldThrowWhenPublishingNonExistentDeck() {
		// Given
		when(this.deckValidationService.evaluate(999L))
				.thenThrow(new DeckNotFoundException(999L));

		// When/Then
		assertThatThrownBy(() -> this.deckService.publish(999L))
				.isInstanceOf(DeckNotFoundException.class);
		verify(this.deckRepository, never()).save(any());
	}

	// ==================== ADD CARD TESTS ====================

	@Test
	@DisplayName("Should add new card to deck section")
	void shouldAddNewCardToDeck() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int quantity = 4;
		final String section = "main";

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardRepository.existsById(cardId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.addCard(deckId, cardId, quantity, section);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(deckId);
		verify(this.deckRepository).existsById(deckId);
		verify(this.cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, section);
		verify(this.cardInDeckRepository).save(any(CardInDeckEntity.class));
	}

	@Test
	@DisplayName("Should increment quantity when adding existing card")
	void shouldIncrementQuantity_WhenAddingExistingCard() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int addQuantity = 2;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(2).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardRepository.existsById(cardId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(existingCard);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.addCard(deckId, cardId, addQuantity, section);

		// Then
		assertThat(result).isNotNull();
		assertThat(existingCard.getQuantity()).isEqualTo(4); // 2 + 2
		verify(this.cardInDeckRepository).save(existingCard);
	}

	@Test
	@DisplayName("Should add card to different sections independently")
	void shouldAddCardToDifferentSections() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardRepository.existsById(cardId)).thenReturn(true);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// Main section
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "main"))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);

		// Sideboard section
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard"))
				.thenReturn(Optional.empty());

		// When
		final Deck result1 = this.deckService.addCard(deckId, cardId, 4, "main");
		final Deck result2 = this.deckService.addCard(deckId, cardId, 2, "sideboard");

		// Then
		assertThat(result1).isNotNull();
		assertThat(result2).isNotNull();
		verify(this.cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, "main");
		verify(this.cardInDeckRepository).findByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard");
		verify(this.cardInDeckRepository, times(2)).save(any(CardInDeckEntity.class));
	}

	@Test
	@DisplayName("Should throw exception when adding card to non-existent deck")
	void shouldThrowException_WhenAddingCardToNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.deckService.addCard(999L, 100L, 4, "main"))
				.isInstanceOf(DeckNotFoundException.class).hasMessageContaining("Deck not found with id: 999");

		verify(this.deckRepository).existsById(999L);
		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should throw exception when adding card with invalid quantity")
	void shouldThrowException_WhenAddingCardWithInvalidQuantity() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then - Zero quantity
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 0, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		// When/Then - Negative quantity
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, -5, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should throw exception when adding card with invalid section")
	void shouldThrowException_WhenAddingCardWithInvalidSection() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then - Invalid section
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 4, "invalid"))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Invalid section");

		// When/Then - Null section
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 4, null))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Invalid section");

		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should accept all valid sections: main, sideboard, maybeboard")
	void shouldAcceptAllValidSections() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardRepository.existsById(cardId)).thenReturn(true);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(eq(deckId), eq(cardId), anyString()))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);

		// When/Then - All valid sections should work
		assertThat(this.deckService.addCard(deckId, cardId, 4, "main")).isNotNull();
		assertThat(this.deckService.addCard(deckId, cardId, 2, "sideboard")).isNotNull();
		assertThat(this.deckService.addCard(deckId, cardId, 1, "maybeboard")).isNotNull();

		verify(this.cardInDeckRepository, times(3)).save(any(CardInDeckEntity.class));
	}

	// ==================== REMOVE CARD TESTS ====================

	@Test
	@DisplayName("Should decrease quantity when removing card")
	void shouldDecreaseQuantity_WhenRemovingCard() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int removeQuantity = 1;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(4).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(existingCard);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.removeCard(deckId, cardId, removeQuantity, section);

		// Then
		assertThat(result).isNotNull();
		assertThat(existingCard.getQuantity()).isEqualTo(3); // 4 - 1
		verify(this.cardInDeckRepository).save(existingCard);
		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should delete card when quantity reaches zero")
	void shouldDeleteCard_WhenQuantityReachesZero() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int removeQuantity = 4;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(4).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.removeCard(deckId, cardId, removeQuantity, section);

		// Then
		assertThat(result).isNotNull();
		verify(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should delete card when removing more than available quantity")
	void shouldDeleteCard_WhenRemovingMoreThanAvailable() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;
		final int removeQuantity = 10;
		final String section = "main";

		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId)
				.quantity(4).section(section).build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.of(existingCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		final Deck result = this.deckService.removeCard(deckId, cardId, removeQuantity, section);

		// Then
		assertThat(result).isNotNull();
		verify(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, section);
	}

	@Test
	@DisplayName("Should throw exception when removing card from non-existent deck")
	void shouldThrowException_WhenRemovingCardFromNonExistentDeck() {
		// Given
		when(this.deckRepository.existsById(999L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.deckService.removeCard(999L, 100L, 1, "main"))
				.isInstanceOf(DeckNotFoundException.class).hasMessageContaining("Deck not found with id: 999");

		verify(this.deckRepository).existsById(999L);
		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should throw exception when removing non-existent card from deck")
	void shouldThrowException_WhenRemovingNonExistentCard() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 999L;
		final String section = "main";

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, section))
				.thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> this.deckService.removeCard(deckId, cardId, 1, section))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Card not found in deck section");

		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should throw exception when removing card with invalid quantity")
	void shouldThrowException_WhenRemovingCardWithInvalidQuantity() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then - Zero quantity
		assertThatThrownBy(() -> this.deckService.removeCard(1L, 100L, 0, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		// When/Then - Negative quantity
		assertThatThrownBy(() -> this.deckService.removeCard(1L, 100L, -5, "main"))
				.isInstanceOf(InvalidDeckCompositionException.class)
				.hasMessageContaining("Quantity must be greater than 0");

		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should throw exception when removing card with invalid section")
	void shouldThrowException_WhenRemovingCardWithInvalidSection() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);

		// When/Then
		assertThatThrownBy(() -> this.deckService.removeCard(1L, 100L, 1, "invalid"))
				.isInstanceOf(InvalidDeckCompositionException.class).hasMessageContaining("Invalid section");

		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(any(), any(), any());
	}

	@Test
	@DisplayName("Should remove card from specific section only")
	void shouldRemoveCardFromSpecificSectionOnly() {
		// Given
		final Long deckId = 1L;
		final Long cardId = 100L;

		final CardInDeckEntity mainCard = CardInDeckEntity.builder().id(1L).deckId(deckId).cardId(cardId).quantity(4)
				.section("main").build();

		when(this.deckRepository.existsById(deckId)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(deckId, cardId, "main"))
				.thenReturn(Optional.of(mainCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "main");
		when(this.deckRepository.findById(deckId)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.removeCard(deckId, cardId, 4, "main");

		// Then - Should only delete from main, not from sideboard
		verify(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "main");
		verify(this.cardInDeckRepository, never()).deleteByDeckIdAndCardIdAndSection(deckId, cardId, "sideboard");
	}

	// ==================== DRAFT STATUS AND MODIFICATION TIME ====================

	@Test
	@DisplayName("Should force draft status when creating a deck")
	void shouldForceDraftStatus_WhenCreatingDeck() {
		// Given
		final Deck newDeck = this.testDeck.toBuilder().id(null).created(null).modified(null).build();
		this.testDeckEntity.setStatus(DeckStatus.PUBLISHED.getValue());
		this.testDeckEntity.setPublishedAt(LocalDateTime.now());

		when(this.deckEntityMapper.toEntity(any(Deck.class))).thenReturn(this.testDeckEntity);
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.create(newDeck);

		// Then
		final ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
		verify(this.deckRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(DeckStatus.DRAFT.getValue());
		assertThat(captor.getValue().getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("Should return a published deck to draft when updating it")
	void shouldReturnDeckToDraft_WhenUpdating() {
		// Given
		this.testDeckEntity.setStatus(DeckStatus.PUBLISHED.getValue());
		this.testDeckEntity.setPublishedAt(LocalDateTime.now());
		final Deck updatedDeck = this.testDeck.toBuilder().name("Updated Deck").build();

		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(updatedDeck);

		// When
		this.deckService.update(1L, updatedDeck);

		// Then
		final ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
		verify(this.deckRepository).save(captor.capture());
		assertThat(captor.getValue().getStatus()).isEqualTo(DeckStatus.DRAFT.getValue());
		assertThat(captor.getValue().getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("Should update the deck modification time when adding a card")
	void shouldUpdateModificationTime_WhenAddingCard() {
		// Given
		final LocalDateTime before = LocalDateTime.of(2020, 1, 1, 0, 0);
		this.testDeckEntity.setModified(before);

		when(this.deckRepository.existsById(1L)).thenReturn(true);
		when(this.cardRepository.existsById(100L)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(1L, 100L, "main"))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.addCard(1L, 100L, 4, "main");

		// Then
		final ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
		verify(this.deckRepository).save(captor.capture());
		assertThat(captor.getValue().getModified()).isAfter(before);
		assertThat(captor.getValue().getStatus()).isEqualTo(DeckStatus.DRAFT.getValue());
		assertThat(captor.getValue().getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("Should return a published deck to draft when adding a card")
	void shouldReturnToDraft_WhenAddingCardToPublishedDeck() {
		// Given
		this.testDeckEntity.setStatus(DeckStatus.PUBLISHED.getValue());
		this.testDeckEntity.setPublishedAt(LocalDateTime.now());

		when(this.deckRepository.existsById(1L)).thenReturn(true);
		when(this.cardRepository.existsById(100L)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(1L, 100L, "main"))
				.thenReturn(Optional.empty());
		when(this.cardInDeckRepository.save(any(CardInDeckEntity.class))).thenReturn(this.testCardInDeck);
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.addCard(1L, 100L, 4, "main");

		// Then
		assertThat(this.testDeckEntity.getStatus()).isEqualTo(DeckStatus.DRAFT.getValue());
		assertThat(this.testDeckEntity.getPublishedAt()).isNull();
	}

	@Test
	@DisplayName("Should reject adding a non-existent card")
	void shouldThrowException_WhenAddingNonExistentCard() {
		// Given
		when(this.deckRepository.existsById(1L)).thenReturn(true);
		when(this.cardRepository.existsById(100L)).thenReturn(false);

		// When/Then
		assertThatThrownBy(() -> this.deckService.addCard(1L, 100L, 4, "main"))
				.isInstanceOf(CardNotFoundException.class)
				.hasMessageContaining("Card not found with id: 100");

		verify(this.cardInDeckRepository, never()).save(any());
	}

	@Test
	@DisplayName("Should update the deck modification time when removing a card")
	void shouldUpdateModificationTime_WhenRemovingCard() {
		// Given
		final LocalDateTime before = LocalDateTime.of(2020, 1, 1, 0, 0);
		this.testDeckEntity.setModified(before);
		final CardInDeckEntity existingCard = CardInDeckEntity.builder().id(1L).deckId(1L).cardId(100L)
				.quantity(4).section("main").build();

		when(this.deckRepository.existsById(1L)).thenReturn(true);
		when(this.cardInDeckRepository.findByDeckIdAndCardIdAndSection(1L, 100L, "main"))
				.thenReturn(Optional.of(existingCard));
		doNothing().when(this.cardInDeckRepository).deleteByDeckIdAndCardIdAndSection(1L, 100L, "main");
		when(this.deckRepository.findById(1L)).thenReturn(Optional.of(this.testDeckEntity));
		when(this.deckRepository.save(any(DeckEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(this.deckEntityMapper.toModel(this.testDeckEntity)).thenReturn(this.testDeck);

		// When
		this.deckService.removeCard(1L, 100L, 4, "main");

		// Then
		final ArgumentCaptor<DeckEntity> captor = ArgumentCaptor.forClass(DeckEntity.class);
		verify(this.deckRepository).save(captor.capture());
		assertThat(captor.getValue().getModified()).isAfter(before);
	}

	private static DeckValidationResult validationResult(DeckIssue... issues) {
		final boolean hasError = Arrays.stream(issues).anyMatch(issue -> issue.getSeverity() == IssueSeverity.ERROR);
		return DeckValidationResult.builder()
				.deckId(1L)
				.status(DeckStatus.DRAFT)
				.validationStatus(hasError ? ValidationStatus.ILLEGAL : ValidationStatus.LEGAL)
				.issues(List.of(issues))
				.build();
	}
}
