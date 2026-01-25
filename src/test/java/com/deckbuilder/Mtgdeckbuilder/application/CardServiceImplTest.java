package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.CardServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.config.PaginationConfig;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Service Implementation Tests")
class CardServiceImplTest {

	@Mock
	private CardRepository cardRepository;

	@Mock
	private CardEntityMapper cardEntityMapper;

	@Mock
	private PaginationConfig paginationConfig;

	@InjectMocks
	private CardServiceImpl cardService;

	private Card testCard;
	private CardEntity testCardEntity;

	@BeforeEach
	void setUp() {
		this.testCard = Card.builder().id(1L).name("Lightning Bolt").manaCost("{R}").cmc(1).colorIdentity("R")
				.typeLine("Instant").cardType("Instant").cardText("Lightning Bolt deals 3 damage to any target.")
				.rarity("Common").power(null).toughness(null).imageUrl("http://example.com/lightning-bolt.jpg")
				.foil(false).unlimitedCopies(false).gameChanger(false).language("en").build();

		this.testCardEntity = new CardEntity();
		this.testCardEntity.setId(1L);
		this.testCardEntity.setName("Lightning Bolt");
		this.testCardEntity.setManaCost("{R}");
		this.testCardEntity.setCmc(1);
		this.testCardEntity.setColorIdentity("R");
		this.testCardEntity.setTypeLine("Instant");
		this.testCardEntity.setCardType("Instant");
		this.testCardEntity.setCardText("Lightning Bolt deals 3 damage to any target.");
		this.testCardEntity.setRarity("Common");
		this.testCardEntity.setImageUrl("http://example.com/lightning-bolt.jpg");
		this.testCardEntity.setFoil(false);
		this.testCardEntity.setUnlimitedCopies(false);
		this.testCardEntity.setGameChanger(false);
		this.testCardEntity.setLanguage("en");
	}

	@Test
	@DisplayName("Should get all cards with pagination")
	void shouldGetAllCards() {
		// Given
		final CardEntity entity2 = new CardEntity();
		entity2.setId(2L);
		entity2.setName("Counterspell");
		entity2.setManaCost("{U}{U}");
		entity2.setTypeLine("Instant");

		final Card card2 = Card.builder().id(2L).name("Counterspell").manaCost("{U}{U}").typeLine("Instant").build();

		final Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(this.testCardEntity, entity2));

		// Mock pagination config
		when(this.paginationConfig.validatePageSize(10)).thenReturn(10);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);

		when(this.cardRepository.findAll(any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);
		when(this.cardEntityMapper.toModel(entity2)).thenReturn(card2);

		// When
		final List<Card> result = this.cardService.getAllCards(10, 0);

		// Then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		assertThat(result.get(1).getName()).isEqualTo("Counterspell");
		verify(this.cardRepository, times(1)).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should get card by ID when exists")
	void shouldGetCardByIdWhenExists() {
		// Given
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.testCardEntity));
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final Optional<Card> result = this.cardService.getCardById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get()).isEqualTo(this.testCard);
		assertThat(result.get().getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findById(1L);
		verify(this.cardEntityMapper, times(1)).toModel(this.testCardEntity);
	}

	@Test
	@DisplayName("Should return empty optional when card not found")
	void shouldReturnEmptyWhenCardNotFound() {
		// Given
		when(this.cardRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Card> result = this.cardService.getCardById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.cardRepository, times(1)).findById(999L);
		verify(this.cardEntityMapper, never()).toModel(any(CardEntity.class));
	}

	@Test
	@DisplayName("Should create card")
	void shouldCreateCard() {
		// Given
		final Card newCard = Card.builder().name("New Card").manaCost("{1}{G}").typeLine("Creature").build();
		final CardEntity newEntity = new CardEntity();
		newEntity.setName("New Card");
		newEntity.setManaCost("{1}{G}");
		newEntity.setTypeLine("Creature");

		final CardEntity savedEntity = new CardEntity();
		savedEntity.setId(3L);
		savedEntity.setName("New Card");
		savedEntity.setManaCost("{1}{G}");
		savedEntity.setTypeLine("Creature");

		final Card savedCard = Card.builder().id(3L).name("New Card").manaCost("{1}{G}").typeLine("Creature").build();

		when(this.cardEntityMapper.toEntity(newCard)).thenReturn(newEntity);
		when(this.cardRepository.save(newEntity)).thenReturn(savedEntity);
		when(this.cardEntityMapper.toModel(savedEntity)).thenReturn(savedCard);

		// When
		final Card result = this.cardService.createCard(newCard);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(3L);
		assertThat(result.getName()).isEqualTo("New Card");
		verify(this.cardEntityMapper, times(1)).toEntity(newCard);
		verify(this.cardRepository, times(1)).save(newEntity);
		verify(this.cardEntityMapper, times(1)).toModel(savedEntity);
	}

	@Test
	@DisplayName("Should update existing card")
	void shouldUpdateExistingCard() {
		// Given
		final Card updatedCard = Card.builder().name("Lightning Bolt Updated").manaCost("{R}").typeLine("Instant")
				.build();
		final CardEntity updatedEntity = new CardEntity();
		updatedEntity.setName("Lightning Bolt Updated");
		updatedEntity.setManaCost("{R}");
		updatedEntity.setTypeLine("Instant");

		final CardEntity savedEntity = new CardEntity();
		savedEntity.setId(1L);
		savedEntity.setName("Lightning Bolt Updated");
		savedEntity.setManaCost("{R}");
		savedEntity.setTypeLine("Instant");

		final Card savedCard = Card.builder().id(1L).name("Lightning Bolt Updated").manaCost("{R}").typeLine("Instant")
				.build();

		when(this.cardRepository.existsById(1L)).thenReturn(true);
		when(this.cardEntityMapper.toEntity(updatedCard)).thenReturn(updatedEntity);
		when(this.cardRepository.save(any(CardEntity.class))).thenReturn(savedEntity);
		when(this.cardEntityMapper.toModel(savedEntity)).thenReturn(savedCard);

		// When
		final Optional<Card> result = this.cardService.updateCard(1L, updatedCard);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Lightning Bolt Updated");
		verify(this.cardRepository, times(1)).existsById(1L);
		verify(this.cardRepository, times(1)).save(any(CardEntity.class));
	}

	@Test
	@DisplayName("Should return empty when updating non-existent card")
	void shouldReturnEmptyWhenUpdatingNonExistentCard() {
		// Given
		final Card updatedCard = Card.builder().name("Non-existent Card").build();
		when(this.cardRepository.existsById(999L)).thenReturn(false);

		// When
		final Optional<Card> result = this.cardService.updateCard(999L, updatedCard);

		// Then
		assertThat(result).isEmpty();
		verify(this.cardRepository, times(1)).existsById(999L);
		verify(this.cardRepository, never()).save(any(CardEntity.class));
	}

	@Test
	@DisplayName("Should delete card")
	void shouldDeleteCard() {
		// Given
		final Long cardId = 1L;
		doNothing().when(this.cardRepository).deleteById(cardId);

		// When
		this.cardService.deleteCard(cardId);

		// Then
		verify(this.cardRepository, times(1)).deleteById(cardId);
	}

	@Test
	@DisplayName("Should search cards by name")
	void shouldSearchCardsByName() {
		// Given
		final Page<CardEntity> entityPage = new PageImpl<>(List.of(this.testCardEntity));

		// Mock pagination config
		when(this.paginationConfig.validatePageSize(10)).thenReturn(10);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);

		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.searchCards("Lightning", 10, 0);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).contains("Lightning");
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	@Test
	@DisplayName("Should get cards by format")
	void shouldGetCardsByFormat() {
		// Given
		final Page<CardEntity> entityPage = new PageImpl<>(List.of(this.testCardEntity));
		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.getCardsByFormat(1L);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	// ========================================
	// Tests for New Card Features
	// ========================================

	@Test
	@DisplayName("Should find cards by collector number")
	void shouldFindCardsByCollectorNumber() {
		// Given
		final String collectorNumber = "001";
		when(this.cardRepository.findByCollectorNumber(collectorNumber)).thenReturn(List.of(this.testCardEntity));
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.findByCollectorNumber(collectorNumber);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findByCollectorNumber(collectorNumber);
	}

	@Test
	@DisplayName("Should find cards by name and set")
	void shouldFindCardsByNameAndSet() {
		// Given
		final String cardName = "Lightning Bolt";
		final Long setId = 1L;
		when(this.cardRepository.findByNameAndCardSet(cardName, setId)).thenReturn(List.of(this.testCardEntity));
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.findByNameAndSet(cardName, setId);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findByNameAndCardSet(cardName, setId);
	}

	@Test
	@DisplayName("Should return parent card when card has parent")
	void shouldReturnParentCardWhenCardHasParent() {
		// Given - Create a face card that has a parent
		final CardEntity faceCardEntity = new CardEntity();
		faceCardEntity.setId(2L);
		faceCardEntity.setName("Beanstalk Giant");
		faceCardEntity.setParentCardId(1L); // This card face points to parent

		final CardEntity parentCardEntity = new CardEntity();
		parentCardEntity.setId(1L);
		parentCardEntity.setName("Beanstalk Giant // Fertile Footsteps");

		final Card parentCard = Card.builder()
			.id(1L)
			.name("Beanstalk Giant // Fertile Footsteps")
			.build();

		when(this.cardRepository.findById(2L)).thenReturn(Optional.of(faceCardEntity));
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(parentCardEntity));
		when(this.cardEntityMapper.toModel(parentCardEntity)).thenReturn(parentCard);

		// When
		final Optional<Card> result = this.cardService.getCardById(2L);

		// Then - Should return the parent card, not the face card
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Beanstalk Giant // Fertile Footsteps");
		verify(this.cardRepository, times(1)).findById(2L);
		verify(this.cardRepository, times(1)).findById(1L);
		verify(this.cardEntityMapper, times(1)).toModel(parentCardEntity);
	}

	@Test
	@DisplayName("Should fallback to face card when parent card not found")
	void shouldFallbackToFaceCardWhenParentNotFound() {
		// Given - Create a face card that has a parent but parent doesn't exist
		final CardEntity faceCardEntity = new CardEntity();
		faceCardEntity.setId(2L);
		faceCardEntity.setName("Beanstalk Giant");
		faceCardEntity.setParentCardId(999L); // Parent doesn't exist

		final Card faceCard = Card.builder()
			.id(2L)
			.name("Beanstalk Giant")
			.build();

		when(this.cardRepository.findById(2L)).thenReturn(Optional.of(faceCardEntity));
		when(this.cardRepository.findById(999L)).thenReturn(Optional.empty());
		when(this.cardEntityMapper.toModel(faceCardEntity)).thenReturn(faceCard);

		// When
		final Optional<Card> result = this.cardService.getCardById(2L);

		// Then - Should fallback to the face card
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(2L);
		assertThat(result.get().getName()).isEqualTo("Beanstalk Giant");
		verify(this.cardRepository, times(1)).findById(2L);
		verify(this.cardRepository, times(1)).findById(999L);
		verify(this.cardEntityMapper, times(1)).toModel(faceCardEntity);
	}

	@Test
	@DisplayName("Should return card without parent when parent ID is null")
	void shouldReturnCardWithoutParentWhenParentIdIsNull() {
		// Given - Create a regular card without parent
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.testCardEntity));
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final Optional<Card> result = this.cardService.getCardById(1L);

		// Then - Should return the card itself
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findById(1L);
		verify(this.cardEntityMapper, times(1)).toModel(this.testCardEntity);
	}

	@Test
	@DisplayName("Should get card by ID required and throw exception when not found")
	void shouldGetCardByIdRequiredAndThrowWhenNotFound() {
		// Given
		when(this.cardRepository.findById(999L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> this.cardService.getCardByIdRequired(999L))
			.isInstanceOf(RuntimeException.class)
			.hasMessageContaining("Card not found");

		verify(this.cardRepository, times(1)).findById(999L);
	}

	@Test
	@DisplayName("Should get card by ID required and return card when found")
	void shouldGetCardByIdRequiredAndReturnWhenFound() {
		// Given
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.testCardEntity));
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final Card result = this.cardService.getCardByIdRequired(1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findById(1L);
		verify(this.cardEntityMapper, times(1)).toModel(this.testCardEntity);
	}

	@Test
	@DisplayName("Should search cards by type")
	void shouldSearchCardsByType() {
		// Given
		final String cardType = "Instant";
		final Page<CardEntity> entityPage = new PageImpl<>(List.of(this.testCardEntity));

		when(this.paginationConfig.validatePageSize(10)).thenReturn(10);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);
		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.searchCardsByType(cardType, 10, 0);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getCardType()).contains(cardType);
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	@Test
	@DisplayName("Should search cards by name with pagination")
	void shouldSearchCardsByNameWithPagination() {
		// Given
		final String cardName = "Lightning";
		final Page<CardEntity> entityPage = new PageImpl<>(List.of(this.testCardEntity));

		when(this.paginationConfig.validatePageSize(5)).thenReturn(5);
		when(this.paginationConfig.validatePageNumber(1)).thenReturn(1);
		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.searchCardsByName(cardName, 5, 1);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).contains(cardName);
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	@Test
	@DisplayName("Should search cards with advanced criteria")
	void shouldSearchCardsWithAdvancedCriteria() {
		// Given
		final String name = "Lightning";
		final String cardType = "Instant";
		final String rarity = "Common";
		final Page<CardEntity> entityPage = new PageImpl<>(List.of(this.testCardEntity));

		when(this.paginationConfig.validatePageSize(10)).thenReturn(10);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);
		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class)))
			.thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.searchCardsAdvanced(name, cardType, rarity, 10, 0);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).contains(name);
		assertThat(result.get(0).getCardType()).isEqualTo(cardType);
		assertThat(result.get(0).getRarity()).isEqualTo(rarity);
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	@Test
	@DisplayName("Should remove duplicates when searching with parent cards")
	void shouldRemoveDuplicatesWhenSearchingWithParentCards() {
		// Given - Two face cards pointing to same parent
		final CardEntity faceCard1 = new CardEntity();
		faceCard1.setId(2L);
		faceCard1.setName("Beanstalk Giant");
		faceCard1.setParentCardId(1L);

		final CardEntity faceCard2 = new CardEntity();
		faceCard2.setId(3L);
		faceCard2.setName("Fertile Footsteps");
		faceCard2.setParentCardId(1L);

		final CardEntity parentCard = new CardEntity();
		parentCard.setId(1L);
		parentCard.setName("Beanstalk Giant // Fertile Footsteps");

		final Card parentCardModel = Card.builder()
			.id(1L)
			.name("Beanstalk Giant // Fertile Footsteps")
			.build();

		final Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(faceCard1, faceCard2));

		when(this.paginationConfig.validatePageSize(10)).thenReturn(10);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);
		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(parentCard));
		when(this.cardEntityMapper.toModel(parentCard)).thenReturn(parentCardModel);

		// When
		final List<Card> result = this.cardService.searchCardsByName("Bean", 10, 0);

		// Then - Should return only one parent card, not two duplicates
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(1L);
		assertThat(result.get(0).getName()).isEqualTo("Beanstalk Giant // Fertile Footsteps");
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	// ========================================
	// Tests for New Search Functionality
	// ========================================

	@Test
	@DisplayName("Should search cards with detailed criteria")
	void shouldSearchCardsWithDetailedCriteria() {
		// Given
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name("Lightning")
			.type("Instant")
			.rarity("common")
			.colors("R")
			.cmcMin(1)
			.cmcMax(3)
			.textContains("damage")
			.language("en")
			.sortBy("name")
			.sortOrder("asc")
			.build();

		final Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(this.testCardEntity), PageRequest.of(0, 20), 1);

		when(this.paginationConfig.validatePageSize(20)).thenReturn(20);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);
		when(this.cardRepository.searchCardsWithDetailedCriteria(eq(criteria), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final CardSearchResult result = this.cardService.searchCardsWithCriteria(criteria, 20, 0);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getCards()).hasSize(1);
		assertThat(result.getTotalCount()).isEqualTo(1);
		assertThat(result.getCards().get(0).getName()).isEqualTo("Lightning Bolt");

		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(eq(criteria), any(Pageable.class));
	}

	@Test
	@DisplayName("Should get random cards without filters")
	void shouldGetRandomCardsWithoutFilters() {
		// Given
		final List<CardEntity> randomEntities = Arrays.asList(this.testCardEntity);
		when(this.cardRepository.findRandomCards(3, null, null, null)).thenReturn(randomEntities);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.getRandomCards(3, null, null, null);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findRandomCards(3, null, null, null);
	}

	@Test
	@DisplayName("Should get random cards with type filter")
	void shouldGetRandomCardsWithTypeFilter() {
		// Given
		final List<CardEntity> randomEntities = Arrays.asList(this.testCardEntity);
		when(this.cardRepository.findRandomCards(5, "Instant", null, null)).thenReturn(randomEntities);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.getRandomCards(5, "Instant", null, null);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findRandomCards(5, "Instant", null, null);
	}

	@Test
	@DisplayName("Should get random cards with rarity and format filters")
	void shouldGetRandomCardsWithRarityAndFormatFilters() {
		// Given
		final List<CardEntity> randomEntities = Arrays.asList(this.testCardEntity);
		when(this.cardRepository.findRandomCards(2, null, "rare", 1L)).thenReturn(randomEntities);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.getRandomCards(2, null, "rare", 1L);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findRandomCards(2, null, "rare", 1L);
	}

	@Test
	@DisplayName("Should handle empty results from detailed search")
	void shouldHandleEmptyResultsFromDetailedSearch() {
		// Given
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name("NonexistentCard")
			.build();

		final Page<CardEntity> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 20), 0);

		when(this.paginationConfig.validatePageSize(20)).thenReturn(20);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);
		when(this.cardRepository.searchCardsWithDetailedCriteria(eq(criteria), any(Pageable.class))).thenReturn(emptyPage);

		// When
		final CardSearchResult result = this.cardService.searchCardsWithCriteria(criteria, 20, 0);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getCards()).isEmpty();
		assertThat(result.getTotalCount()).isEqualTo(0);

		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(eq(criteria), any(Pageable.class));
	}

	@Test
	@DisplayName("Should remove duplicates from random card results")
	void shouldRemoveDuplicatesFromRandomCardResults() {
		// Given - Setup two face cards that point to the same parent
		final CardEntity faceCard1 = new CardEntity();
		faceCard1.setId(2L);
		faceCard1.setName("Beanstalk Giant");
		faceCard1.setParentCardId(1L);

		final CardEntity faceCard2 = new CardEntity();
		faceCard2.setId(3L);
		faceCard2.setName("Fertile Footsteps");
		faceCard2.setParentCardId(1L);

		final CardEntity parentCard = new CardEntity();
		parentCard.setId(1L);
		parentCard.setName("Beanstalk Giant // Fertile Footsteps");

		final Card parentCardModel = Card.builder()
			.id(1L)
			.name("Beanstalk Giant // Fertile Footsteps")
			.build();

		when(this.cardRepository.findRandomCards(2, null, null, null)).thenReturn(Arrays.asList(faceCard1, faceCard2));
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(parentCard));
		when(this.cardEntityMapper.toModel(parentCard)).thenReturn(parentCardModel);

		// When
		final List<Card> result = this.cardService.getRandomCards(2, null, null, null);

		// Then - Should return only one parent card, not two duplicates
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(1L);
		assertThat(result.get(0).getName()).isEqualTo("Beanstalk Giant // Fertile Footsteps");
		verify(this.cardRepository, times(1)).findRandomCards(2, null, null, null);
	}

	@Test
	@DisplayName("Should search with complex criteria including CMC ranges")
	void shouldSearchWithComplexCriteriaIncludingCmcRanges() {
		// Given
		final CardSearchCriteria criteria = CardSearchCriteria.builder()
			.name("Lightning")
			.type("Instant")
			.cmcMin(1)
			.cmcMax(2)
			.colors("R")
			.textContains("damage")
			.isFoil(false)
			.isPromo(false)
			.language("en")
			.sortBy("cmc")
			.sortOrder("desc")
			.build();

		final Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(this.testCardEntity), PageRequest.of(0, 10), 1);

		when(this.paginationConfig.validatePageSize(10)).thenReturn(10);
		when(this.paginationConfig.validatePageNumber(0)).thenReturn(0);
		when(this.cardRepository.searchCardsWithDetailedCriteria(eq(criteria), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final CardSearchResult result = this.cardService.searchCardsWithCriteria(criteria, 10, 0);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getCards()).hasSize(1);
		assertThat(result.getTotalCount()).isEqualTo(1);
		assertThat(result.getCards().get(0).getName()).isEqualTo("Lightning Bolt");

		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(eq(criteria), any(Pageable.class));
	}
}
