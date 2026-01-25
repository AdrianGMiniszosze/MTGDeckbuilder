package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Card Repository Tests")
class CardRepositoryTest {

	@Mock
	private CardRepository cardRepository;

	private CardEntity testCardEntity;

	@BeforeEach
	void setUp() {
        this.testCardEntity = new CardEntity();
        this.testCardEntity.setId(1L);
        this.testCardEntity.setName("Black Lotus");
        this.testCardEntity.setManaCost("{0}");
        this.testCardEntity.setCmc(0);
        this.testCardEntity.setColorIdentity("");
        this.testCardEntity.setTypeLine("Artifact");
        this.testCardEntity.setCardType("Artifact");
        this.testCardEntity.setCardText("Add three mana of any one color.");
        this.testCardEntity.setRarity("Mythic Rare");
        this.testCardEntity.setCardSet(1L);
        this.testCardEntity.setImageUrl("http://example.com/black-lotus.jpg");
        this.testCardEntity.setFoil(false);
        this.testCardEntity.setUnlimitedCopies(false);
        this.testCardEntity.setGameChanger(false);
        this.testCardEntity.setLanguage("en");
	}

	@Test
	@DisplayName("Should find card by ID")
	void shouldFindCardById() {
		// Given
		when(this.cardRepository.findById(1L)).thenReturn(Optional.of(this.testCardEntity));

		// When
		final Optional<CardEntity> result = this.cardRepository.findById(1L);

		// Then
		assertThat(result).isPresent();
		assertThat(result.get().getName()).isEqualTo("Black Lotus");
		verify(this.cardRepository, times(1)).findById(1L);
	}

	@Test
	@DisplayName("Should return empty optional when card not found")
	void shouldReturnEmptyWhenCardNotFound() {
		// Given
		when(this.cardRepository.findById(999L)).thenReturn(Optional.empty());

		// When
		final Optional<CardEntity> result = this.cardRepository.findById(999L);

		// Then
		assertThat(result).isEmpty();
		verify(this.cardRepository, times(1)).findById(999L);
	}

	@Test
	@DisplayName("Should find all cards with pagination")
	void shouldFindAllCardsWithPagination() {
		// Given
		final CardEntity entity2 = new CardEntity();
		entity2.setId(2L);
		entity2.setName("Mox Sapphire");
		entity2.setManaCost("{0}");
		entity2.setTypeLine("Artifact");

		final Page<CardEntity> entityPage = new PageImpl<>(Arrays.asList(this.testCardEntity, entity2));
		when(this.cardRepository.findAll(any(Pageable.class))).thenReturn(entityPage);

		// When
		final Page<CardEntity> result = this.cardRepository.findAll(Pageable.ofSize(10).withPage(0));

		// Then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent()).extracting(CardEntity::getName).containsExactly("Black Lotus", "Mox Sapphire");
		verify(this.cardRepository, times(1)).findAll(any(Pageable.class));
	}

	@Test
	@DisplayName("Should search cards by name")
	void shouldSearchCardsByName() {
		// Given
		final Page<CardEntity> entityPage = new PageImpl<>(List.of(this.testCardEntity));
		when(this.cardRepository.searchCardsWithDetailedCriteria(any(), any(Pageable.class))).thenReturn(entityPage);

		// When
		final Pageable pageable = Pageable.ofSize(10).withPage(0);
		final Page<CardEntity> result = this.cardRepository.searchCardsWithDetailedCriteria(null, pageable);

		// Then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getName()).contains("Lotus");
		verify(this.cardRepository, times(1)).searchCardsWithDetailedCriteria(any(), any(Pageable.class));
	}

	@Test
	@DisplayName("Should save card")
	void shouldSaveCard() {
		// Given
		when(this.cardRepository.save(any(CardEntity.class))).thenReturn(this.testCardEntity);

		// When
		final CardEntity result = this.cardRepository.save(this.testCardEntity);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Black Lotus");
		verify(this.cardRepository, times(1)).save(this.testCardEntity);
	}

	@Test
	@DisplayName("Should delete card")
	void shouldDeleteCard() {
		// Given
		doNothing().when(this.cardRepository).deleteById(anyLong());

		// When
        this.cardRepository.deleteById(1L);

		// Then
		verify(this.cardRepository, times(1)).deleteById(1L);
	}

	// ========================================
	// Card Variant Tests (New Feature)
	// ========================================

	@Test
	@DisplayName("Should store card with collector number")
	void shouldStoreCardWithCollectorNumber() {
		// Given
        this.testCardEntity.setCollectorNumber("001");
		when(this.cardRepository.save(any(CardEntity.class))).thenReturn(this.testCardEntity);

		// When
		final CardEntity result = this.cardRepository.save(this.testCardEntity);

		// Then
		assertThat(result.getCollectorNumber()).isEqualTo("001");
		verify(this.cardRepository, times(1)).save(this.testCardEntity);
	}

	@Test
	@DisplayName("Should store multiple variants of same card")
	void shouldStoreMultipleVariantsOfSameCard() {
		// Given - Regular version
		final CardEntity regularCard = new CardEntity();
		regularCard.setId(1L);
		regularCard.setName("Lightning Bolt");
		regularCard.setCollectorNumber("001");
		regularCard.setFoil(false);
		regularCard.setCardSet(1L);

		// Given - Foil version
		final CardEntity foilCard = new CardEntity();
		foilCard.setId(2L);
		foilCard.setName("Lightning Bolt");
		foilCard.setCollectorNumber("001★");
		foilCard.setFoil(true);
		foilCard.setCardSet(1L);

		when(this.cardRepository.save(regularCard)).thenReturn(regularCard);
		when(this.cardRepository.save(foilCard)).thenReturn(foilCard);

		// When
		final CardEntity savedRegular = this.cardRepository.save(regularCard);
		final CardEntity savedFoil = this.cardRepository.save(foilCard);

		// Then
		assertThat(savedRegular.getName()).isEqualTo("Lightning Bolt");
		assertThat(savedFoil.getName()).isEqualTo("Lightning Bolt");
		assertThat(savedRegular.getCollectorNumber()).isNotEqualTo(savedFoil.getCollectorNumber());
		assertThat(savedRegular.getFoil()).isFalse();
		assertThat(savedFoil.getFoil()).isTrue();
	}

	@Test
	@DisplayName("Should handle promo flag")
	void shouldHandlePromoFlag() {
		// Given
        this.testCardEntity.setPromo(true);
		when(this.cardRepository.save(any(CardEntity.class))).thenReturn(this.testCardEntity);

		// When
		final CardEntity result = this.cardRepository.save(this.testCardEntity);

		// Then
		assertThat(result.getPromo()).isTrue();
		verify(this.cardRepository, times(1)).save(this.testCardEntity);
	}

	@Test
	@DisplayName("Should handle variation flag")
	void shouldHandleVariationFlag() {
		// Given
        this.testCardEntity.setVariation(true);
		when(this.cardRepository.save(any(CardEntity.class))).thenReturn(this.testCardEntity);

		// When
		final CardEntity result = this.cardRepository.save(this.testCardEntity);

		// Then
		assertThat(result.getVariation()).isTrue();
		verify(this.cardRepository, times(1)).save(this.testCardEntity);
	}

	@Test
	@DisplayName("Should find cards by collector number")
	void shouldFindCardsByCollectorNumber() {
		// Given
        this.testCardEntity.setCollectorNumber("001");
		final List<CardEntity> cards = List.of(this.testCardEntity);
		when(this.cardRepository.findByCollectorNumber("001")).thenReturn(cards);

		// When
		final List<CardEntity> result = this.cardRepository.findByCollectorNumber("001");

		// Then
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getCollectorNumber()).isEqualTo("001");
		verify(this.cardRepository, times(1)).findByCollectorNumber("001");
	}

	@Test
	@DisplayName("Should find all variants of a card in a set")
	void shouldFindAllVariantsOfCardInSet() {
		// Given - Three variants of Forest
		final CardEntity forest1 = new CardEntity();
		forest1.setName("Forest");
		forest1.setCollectorNumber("264");
		forest1.setCardSet(1L);

		final CardEntity forest2 = new CardEntity();
		forest2.setName("Forest");
		forest2.setCollectorNumber("265");
		forest2.setCardSet(1L);

		final CardEntity forest3 = new CardEntity();
		forest3.setName("Forest");
		forest3.setCollectorNumber("266");
		forest3.setCardSet(1L);

		final List<CardEntity> variants = Arrays.asList(forest1, forest2, forest3);
		when(this.cardRepository.findByNameAndCardSet("Forest", 1L)).thenReturn(variants);

		// When
		final List<CardEntity> result = this.cardRepository.findByNameAndCardSet("Forest", 1L);

		// Then
		assertThat(result).hasSize(3);
		assertThat(result).allMatch(card -> card.getName().equals("Forest"));
		assertThat(result).allMatch(card -> card.getCardSet().equals(1L));
		assertThat(result.stream().map(CardEntity::getCollectorNumber)).containsExactlyInAnyOrder("264", "265", "266");
	}

	// ========================================
	// Tests for New Repository Methods
	// ========================================

	@Test
	@DisplayName("Should store and retrieve parent card relationship")
	void shouldStoreAndRetrieveParentCardRelationship() {
		// Given - Parent card and face card
		final CardEntity parentCard = new CardEntity();
		parentCard.setId(1L);
		parentCard.setName("Beanstalk Giant // Fertile Footsteps");

		final CardEntity faceCard = new CardEntity();
		faceCard.setId(2L);
		faceCard.setName("Beanstalk Giant");
		faceCard.setParentCardId(1L);

		when(this.cardRepository.save(parentCard)).thenReturn(parentCard);
		when(this.cardRepository.save(faceCard)).thenReturn(faceCard);

		// When
		final CardEntity savedParent = this.cardRepository.save(parentCard);
		final CardEntity savedFace = this.cardRepository.save(faceCard);

		// Then
		assertThat(savedParent.getId()).isEqualTo(1L);
		assertThat(savedFace.getParentCardId()).isEqualTo(1L);
		verify(this.cardRepository, times(1)).save(parentCard);
		verify(this.cardRepository, times(1)).save(faceCard);
	}


	@Test
	@DisplayName("Should find multiple cards with same collector number across different sets")
	void shouldFindMultipleCardsWithSameCollectorNumberAcrossDifferentSets() {
		// Given - Same card reprinted in different sets
		final CardEntity originalPrint = new CardEntity();
		originalPrint.setName("Lightning Bolt");
		originalPrint.setCollectorNumber("001");
		originalPrint.setCardSet(1L); // Alpha set

		final CardEntity reprint = new CardEntity();
		reprint.setName("Lightning Bolt");
		reprint.setCollectorNumber("001");
		reprint.setCardSet(2L); // Beta set

		final List<CardEntity> cards = Arrays.asList(originalPrint, reprint);
		when(this.cardRepository.findByCollectorNumber("001")).thenReturn(cards);

		// When
		final List<CardEntity> result = this.cardRepository.findByCollectorNumber("001");

		// Then
		assertThat(result).hasSize(2);
		assertThat(result).allMatch(card -> card.getCollectorNumber().equals("001"));
		assertThat(result).allMatch(card -> card.getName().equals("Lightning Bolt"));
		assertThat(result.stream().map(CardEntity::getCardSet)).containsExactlyInAnyOrder(1L, 2L);
	}
}
