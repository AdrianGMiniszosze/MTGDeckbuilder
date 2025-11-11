package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.application.implement.CardServiceImpl;
import com.deckbuilder.mtgdeckbuilder.infrastructure.CardRepository;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
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

	@InjectMocks
	private CardServiceImpl cardService;

	private Card testCard;
	private CardEntity testCardEntity;

	@BeforeEach
	void setUp() {
        this.testCard = Card.builder().id(1L).name("Lightning Bolt").manaCost("{R}").cmc(1).colorIdentity("R")
				.typeLine("Instant").cardType("Instant").oracleText("Lightning Bolt deals 3 damage to any target.")
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
        this.testCardEntity.setOracleText("Lightning Bolt deals 3 damage to any target.");
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
		final Card updatedCard = Card.builder().name("Lightning Bolt Updated").manaCost("{R}").typeLine("Instant").build();
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

		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.testCardEntity));
		when(this.cardEntityMapper.toEntity(updatedCard)).thenReturn(updatedEntity);
		when(this.cardRepository.save(any(CardEntity.class))).thenReturn(savedEntity);
		when(this.cardEntityMapper.toModel(savedEntity)).thenReturn(savedCard);

		// When
		final Optional<Card> result = this.cardService.updateCard(1L, updatedCard);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getId()).isEqualTo(1L);
		assertThat(result.get().getName()).isEqualTo("Lightning Bolt Updated");
		verify(this.cardRepository, times(1)).findById(1L);
		verify(this.cardRepository, times(1)).save(any(CardEntity.class));
	}

	@Test
	@DisplayName("Should return empty when updating non-existent card")
	void shouldReturnEmptyWhenUpdatingNonExistentCard() {
		// Given
		final Card updatedCard = Card.builder().name("Non-existent Card").build();
		when(this.cardRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<Card> result = this.cardService.updateCard(999L, updatedCard);

		// Then
		assertThat(result).isEmpty();
		verify(this.cardRepository, times(1)).findById(999L);
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
		when(this.cardRepository.searchByName(eq("Lightning"), any(Pageable.class))).thenReturn(entityPage);
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.searchCards("Lightning", 10, 0);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).contains("Lightning");
		verify(this.cardRepository, times(1)).searchByName(eq("Lightning"), any(Pageable.class));
	}

	@Test
	@DisplayName("Should get cards by format")
	void shouldGetCardsByFormat() {
		// Given
		when(this.cardRepository.findByFormatId(1L)).thenReturn(List.of(this.testCardEntity));
		when(this.cardEntityMapper.toModel(this.testCardEntity)).thenReturn(this.testCard);

		// When
		final List<Card> result = this.cardService.getCardsByFormat(1L);

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("Lightning Bolt");
		verify(this.cardRepository, times(1)).findByFormatId(1L);
	}

	@Test
	@DisplayName("Should throw UnsupportedOperationException for findSimilarCards")
	void shouldThrowExceptionForFindSimilarCards() {
		// Given
		final Double[] vector = new Double[1536];

		// When & Then
		assertThatThrownBy(() -> this.cardService.findSimilarCards(vector, 10))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Not implemented yet");
	}

	@Test
	@DisplayName("Should throw UnsupportedOperationException for findSimilarToCard")
	void shouldThrowExceptionForFindSimilarToCard() {
		// When & Then
		assertThatThrownBy(() -> this.cardService.findSimilarToCard(1L, 10))
				.isInstanceOf(UnsupportedOperationException.class).hasMessage("Not implemented yet");
	}

}
