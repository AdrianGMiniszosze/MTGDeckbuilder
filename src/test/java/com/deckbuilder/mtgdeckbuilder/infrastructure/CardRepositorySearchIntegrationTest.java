package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardLegalityEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.CardSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the dynamic Criteria API search and the native random query against a
 * real PostgreSQL instance, which is the only way to check generated SQL.
 */
@DisplayName("Card Repository Search Integration Tests")
class CardRepositorySearchIntegrationTest extends PostgresDataJpaTest {

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardLegalityRepository cardLegalityRepository;

    @Autowired
    private FormatRepository formatRepository;

    private CardEntity lightningBolt;
    private CardEntity counterspell;
    private FormatEntity modern;

    @BeforeEach
    void setUp() {
        lightningBolt = cardRepository.save(card("Lightning Bolt", "Instant", "common", 1, "R",
                "Deals 3 damage to any target.", null, null, 1L, false, false, "en"));
        cardRepository.save(card("Mountain", "Land", "common", 0, "R",
                "", null, null, 1L, false, false, "en"));
        CardEntity serraAngel = card("Serra Angel", "Creature", "uncommon", 5, "W",
                "Flying, vigilance.", "4", "4", 1L, false, true, "es");
        cardRepository.save(serraAngel);
        cardRepository.save(card("Llanowar Elves", "Creature", "common", 1, "G",
                "Add one green mana.", "1", "1", 2L, true, false, "en"));
        counterspell = card("Counterspell", "Instant", "rare", 2, "U",
                "Counter target spell.", null, null, 2L, false, false, "en");
        cardRepository.save(counterspell);
        cardRepository.flush();

        modern = formatRepository.save(FormatEntity.builder().name("Modern Search").description("test").build());
        formatRepository.flush();

        CardLegalityEntity legality = new CardLegalityEntity();
        legality.setCardId(lightningBolt.getId());
        legality.setFormatId(modern.getId());
        legality.setLegalityStatus("legal");
        cardLegalityRepository.save(legality);
        cardLegalityRepository.flush();
    }

    private CardEntity card(String name, String type, String rarity, int cmc, String colorIdentity, String text,
            String power, String toughness, Long setId, boolean foil, boolean promo, String language) {
        CardEntity card = new CardEntity();
        card.setName(name);
        card.setCardType(type);
        card.setTypeLine(type);
        card.setRarity(rarity);
        card.setCmc(cmc);
        card.setColorIdentity(colorIdentity);
        card.setCardText(text);
        card.setPower(power);
        card.setToughness(toughness);
        card.setCardSetId(setId);
        card.setFoil(foil);
        card.setPromo(promo);
        card.setLanguage(language);
        card.setImageUrl("http://example.com/" + name.replace(' ', '-') + ".jpg");
        card.setScryfallId(UUID.randomUUID());
        card.setOracleId(UUID.randomUUID());
        return card;
    }

    private Page<CardEntity> search(CardSearchCriteria criteria) {
        return cardRepository.searchCardsWithDetailedCriteria(criteria, PageRequest.of(0, 10));
    }

    private List<String> names(Page<CardEntity> page) {
        return page.getContent().stream().map(CardEntity::getName).toList();
    }

    @Nested
    @DisplayName("Criteria search")
    class CriteriaSearchTests {

        @Test
        @DisplayName("Should return every card sorted by name when nothing is filtered")
        void shouldReturnAllCardsSortedByName() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().build());

            // Then
            assertThat(names(result)).containsExactly("Counterspell", "Lightning Bolt", "Llanowar Elves", "Mountain",
                    "Serra Angel");
            assertThat(result.getTotalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should paginate and keep the total count of matches")
        void shouldPaginate() {
            // Given
            final Pageable pageable = PageRequest.of(1, 2);

            // When
            final Page<CardEntity> result = cardRepository
                    .searchCardsWithDetailedCriteria(CardSearchCriteria.builder().build(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should filter by name ignoring case and surrounding text")
        void shouldFilterByName() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().name("LIGHTNING").build());

            // Then
            assertThat(names(result)).containsExactly("Lightning Bolt");
        }

        @Test
        @DisplayName("Should filter by card type")
        void shouldFilterByType() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().type("creature").build());

            // Then
            assertThat(names(result)).containsExactlyInAnyOrder("Llanowar Elves", "Serra Angel");
        }

        @Test
        @DisplayName("Should filter by exact rarity")
        void shouldFilterByRarity() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().rarity("RARE").build());

            // Then
            assertThat(names(result)).containsExactly("Counterspell");
        }

        @Test
        @DisplayName("Should filter by color identity substring")
        void shouldFilterByColors() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().colors("U").build());

            // Then
            assertThat(names(result)).containsExactly("Counterspell");
        }

        @Test
        @DisplayName("Should filter by mana value range")
        void shouldFilterByCmcRange() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().cmcMin(1).cmcMax(2).build());

            // Then
            assertThat(names(result)).containsExactlyInAnyOrder("Lightning Bolt", "Llanowar Elves", "Counterspell");
        }

        @Test
        @DisplayName("Should filter by set")
        void shouldFilterBySet() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().setId(2L).build());

            // Then
            assertThat(names(result)).containsExactlyInAnyOrder("Llanowar Elves", "Counterspell");
        }

        @Test
        @DisplayName("Should filter by text content ignoring case")
        void shouldFilterByText() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().textContains("DAMAGE").build());

            // Then
            assertThat(names(result)).containsExactly("Lightning Bolt");
        }

        @Test
        @DisplayName("Should filter by power and toughness")
        void shouldFilterByPowerAndToughness() {
            // When / Then
            assertThat(names(search(CardSearchCriteria.builder().powerMin("2").build())))
                    .containsExactly("Serra Angel");
            assertThat(names(search(CardSearchCriteria.builder().powerMax("1").build())))
                    .containsExactly("Llanowar Elves");
            assertThat(names(search(CardSearchCriteria.builder().toughnessMin("4").build())))
                    .containsExactly("Serra Angel");
            assertThat(names(search(CardSearchCriteria.builder().toughnessMax("1").build())))
                    .containsExactly("Llanowar Elves");
        }

        @Test
        @DisplayName("Should ignore non numeric power and toughness bounds")
        void shouldIgnoreInvalidNumericBounds() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder()
                    .powerMin("abc").powerMax("abc").toughnessMin("x").toughnessMax("x").build());

            // Then - the filters are dropped instead of throwing
            assertThat(result.getTotalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should filter by foil, promo and language")
        void shouldFilterByFlagsAndLanguage() {
            // When / Then
            assertThat(names(search(CardSearchCriteria.builder().isFoil(true).build())))
                    .containsExactly("Llanowar Elves");
            assertThat(names(search(CardSearchCriteria.builder().isPromo(true).build())))
                    .containsExactly("Serra Angel");
            assertThat(names(search(CardSearchCriteria.builder().language("es").build())))
                    .containsExactly("Serra Angel");
        }

        @Test
        @DisplayName("Should return an empty page with zero total when nothing matches")
        void shouldReturnEmptyPage() {
            // When
            final Page<CardEntity> result = search(CardSearchCriteria.builder().name("nothing here").build());

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("Should sort by the requested column and direction")
        void shouldSortByRequestedColumn() {
            // When / Then
            assertThat(names(search(CardSearchCriteria.builder().sortBy("cmc").sortOrder("desc").build())).get(0))
                    .isEqualTo("Serra Angel");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("type").sortOrder("asc").build()))
                    .subList(0, 2))
                    .containsExactlyInAnyOrder("Serra Angel", "Llanowar Elves");
            // PostgreSQL puts NULLs first on DESC, so the non-null powers close it
            assertThat(names(search(CardSearchCriteria.builder().sortBy("power").sortOrder("desc").build())))
                    .endsWith("Serra Angel", "Llanowar Elves");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("toughness").sortOrder("asc").build())).get(0))
                    .isEqualTo("Llanowar Elves");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("set").sortOrder("desc").build()))
                    .subList(0, 2))
                    .containsExactlyInAnyOrder("Llanowar Elves", "Counterspell");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("unknown").build())).get(0))
                    .isEqualTo("Counterspell");
        }

        @Test
        @DisplayName("Should sort in both directions for every column")
        void shouldSortInBothDirections() {
            // When / Then
            assertThat(names(search(CardSearchCriteria.builder().sortBy("name").sortOrder("desc").build())).get(0))
                    .isEqualTo("Serra Angel");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("cmc").sortOrder("asc").build())).get(0))
                    .isEqualTo("Mountain");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("rarity").sortOrder("desc").build())).get(0))
                    .isEqualTo("Serra Angel");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("type").sortOrder("desc").build())).get(0))
                    .isEqualTo("Mountain");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("power").sortOrder("asc").build())).get(0))
                    .isEqualTo("Llanowar Elves");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("toughness").sortOrder("asc").build())).get(0))
                    .isEqualTo("Llanowar Elves");
            assertThat(names(search(CardSearchCriteria.builder().sortBy("set").sortOrder("asc").build())).subList(0, 3))
                    .containsExactlyInAnyOrder("Lightning Bolt", "Mountain", "Serra Angel");
        }

        @Test
        @DisplayName("Should only log the format filter in the criteria path")
        void shouldTreatFormatFilterAsPredicate() {
            // When - format filtering by criteria is still a TODO in the class,
            // so the flag alone does not narrow the result
            final Page<CardEntity> result = search(CardSearchCriteria.builder().formatId(modern.getId()).build());

            // Then
            assertThat(result.getTotalElements()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Random card query")
    class RandomCardTests {

        @Test
        @DisplayName("Should return the requested number of random cards")
        void shouldReturnRequestedCount() {
            // When
            final List<CardEntity> result = cardRepository.findRandomCards(3, null, null, null);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(CardEntity::getId).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("Should filter random cards by type and rarity")
        void shouldFilterByTypeAndRarity() {
            // When
            final List<CardEntity> creatures = cardRepository.findRandomCards(10, "Creature", null, null);
            final List<CardEntity> rares = cardRepository.findRandomCards(10, null, "rare", null);

            // Then
            assertThat(creatures).extracting(CardEntity::getName)
                    .containsExactlyInAnyOrder("Serra Angel", "Llanowar Elves");
            assertThat(rares).extracting(CardEntity::getName).containsExactly("Counterspell");
        }

        @Test
        @DisplayName("Should filter random cards by format through card legality")
        void shouldFilterByFormat() {
            // When
            final List<CardEntity> result = cardRepository.findRandomCards(10, null, null, modern.getId());

            // Then
            assertThat(result).extracting(CardEntity::getName).containsExactly("Lightning Bolt");
        }

        @Test
        @DisplayName("Should combine type, rarity and format filters")
        void shouldCombineFilters() {
            // When
            final List<CardEntity> match = cardRepository.findRandomCards(10, "Instant", "common", modern.getId());
            final List<CardEntity> mismatch = cardRepository.findRandomCards(10, "Creature", null, modern.getId());

            // Then
            assertThat(match).extracting(CardEntity::getName).containsExactly("Lightning Bolt");
            assertThat(mismatch).isEmpty();
        }

        @Test
        @DisplayName("Should return nothing when the limit is zero")
        void shouldReturnNothingForZeroLimit() {
            // When
            final List<CardEntity> result = cardRepository.findRandomCards(0, null, null, null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should bind filters as parameters instead of concatenating SQL")
        void shouldNotConcatenateFiltersIntoTheQuery() {
            // Given - a classic injection payload now treated as a LIKE pattern
            final String payload = "' OR 1=1 --";

            // When
            final List<CardEntity> typeResult = cardRepository.findRandomCards(10, payload, null, null);
            final List<CardEntity> rarityResult = cardRepository.findRandomCards(10, null, payload, null);

            // Then - no match, and the table is still there with all its rows
            assertThat(typeResult).isEmpty();
            assertThat(rarityResult).isEmpty();
            assertThat(cardRepository.count()).isEqualTo(5);
        }
    }
}
