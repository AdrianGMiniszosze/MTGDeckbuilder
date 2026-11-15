package com.deckbuilder.mtgdeckbuilder.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Card Search Criteria Tests")
class CardSearchCriteriaTest {

    @Test
    @DisplayName("Should report no filters when only sorting is set")
    void shouldReportNoFilters() {
        assertThat(CardSearchCriteria.builder().build().hasFilters()).isFalse();
        assertThat(CardSearchCriteria.builder().sortBy("name").sortOrder("asc").build().hasFilters()).isFalse();
    }

    @Test
    @DisplayName("Should detect every supported filter")
    void shouldDetectEveryFilter() {
        final List<CardSearchCriteria> filters = List.of(CardSearchCriteria.builder().name("bolt").build(),
                CardSearchCriteria.builder().type("creature").build(),
                CardSearchCriteria.builder().rarity("rare").build(),
                CardSearchCriteria.builder().colors("R").build(),
                CardSearchCriteria.builder().cmcMin(1).build(),
                CardSearchCriteria.builder().cmcMax(3).build(),
                CardSearchCriteria.builder().powerMin("1").build(),
                CardSearchCriteria.builder().powerMax("9").build(),
                CardSearchCriteria.builder().toughnessMin("1").build(),
                CardSearchCriteria.builder().toughnessMax("9").build(),
                CardSearchCriteria.builder().setId(1L).build(),
                CardSearchCriteria.builder().formatId(1L).build(),
                CardSearchCriteria.builder().textContains("damage").build(),
                CardSearchCriteria.builder().keywords("flying").build(),
                CardSearchCriteria.builder().isFoil(false).build(),
                CardSearchCriteria.builder().isPromo(true).build());

        assertThat(filters).allMatch(CardSearchCriteria::hasFilters);
    }

    @Test
    @DisplayName("Should ignore the default language but honour any other")
    void shouldTreatNonDefaultLanguageAsFilter() {
        assertThat(CardSearchCriteria.builder().language("en").build().hasFilters()).isFalse();
        assertThat(CardSearchCriteria.builder().language("es").build().hasFilters()).isTrue();
    }
}
