package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Composite keys are compared by Hibernate when it looks entities up, so their
 * equals/hashCode contract has to hold for every combination.
 */
@DisplayName("Composite Key Tests")
class CompositeKeyTest {

    @Test
    @DisplayName("Should compare card face keys by value")
    void shouldCompareCardFaceIds() {
        final CardFaceId key = new CardFaceId(1L, (short) 1);

        assertThat(key.equals(key)).isTrue();
        assertThat(key.equals(null)).isFalse();
        assertThat(key.equals("card")).isFalse();
        assertThat(key).isEqualTo(new CardFaceId(1L, (short) 1));
        assertThat(key).isNotEqualTo(new CardFaceId(1L, (short) 2));
        assertThat(key).isNotEqualTo(new CardFaceId(2L, (short) 1));
        assertThat(key).hasSameHashCodeAs(new CardFaceId(1L, (short) 1));
        assertThat(key).isNotEqualTo(new CardFaceId(null, null));
    }

    @Test
    @DisplayName("Should compare card legality keys by value")
    void shouldCompareCardLegalityIds() {
        final CardLegalityId key = new CardLegalityId(1L, 2L);

        assertThat(key.equals(key)).isTrue();
        assertThat(key.equals(null)).isFalse();
        assertThat(key.equals("card")).isFalse();
        assertThat(key).isEqualTo(new CardLegalityId(1L, 2L));
        assertThat(key).isNotEqualTo(new CardLegalityId(1L, 3L));
        assertThat(key).isNotEqualTo(new CardLegalityId(3L, 2L));
        assertThat(key).hasSameHashCodeAs(new CardLegalityId(1L, 2L));
        assertThat(key).isNotEqualTo(new CardLegalityId(null, null));
    }

    @Test
    @DisplayName("Should compare card tag keys by value")
    void shouldCompareCardTagIds() {
        final CardTagId key = new CardTagId(1L, 2L);

        assertThat(key.equals(key)).isTrue();
        assertThat(key.equals(null)).isFalse();
        assertThat(key.equals("card")).isFalse();
        assertThat(key).isEqualTo(new CardTagId(1L, 2L));
        assertThat(key).isNotEqualTo(new CardTagId(1L, 3L));
        assertThat(key).isNotEqualTo(new CardTagId(3L, 2L));
        assertThat(key).hasSameHashCodeAs(new CardTagId(1L, 2L));
        assertThat(key).isNotEqualTo(new CardTagId(null, null));
    }

    @Test
    @DisplayName("Should compare card ruling keys by value")
    void shouldCompareCardRulingIds() {
        final UUID oracleId = UUID.randomUUID();
        final LocalDate publishedAt = LocalDate.of(2024, 1, 1);
        final CardRulingId key = new CardRulingId(oracleId, publishedAt, "Ruling text", "wotc");

        assertThat(key.equals(key)).isTrue();
        assertThat(key.equals(null)).isFalse();
        assertThat(key.equals("card")).isFalse();
        assertThat(key).isEqualTo(new CardRulingId(oracleId, publishedAt, "Ruling text", "wotc"));
        assertThat(key).isNotEqualTo(new CardRulingId(UUID.randomUUID(), publishedAt, "Ruling text", "wotc"));
        assertThat(key).isNotEqualTo(new CardRulingId(oracleId, publishedAt.plusDays(1), "Ruling text", "wotc"));
        assertThat(key).isNotEqualTo(new CardRulingId(oracleId, publishedAt, "Other text", "wotc"));
        assertThat(key).isNotEqualTo(new CardRulingId(oracleId, publishedAt, "Ruling text", "scryfall"));
        assertThat(key).hasSameHashCodeAs(new CardRulingId(oracleId, publishedAt, "Ruling text", "wotc"));
        assertThat(key).isNotEqualTo(new CardRulingId(null, null, null, null));
    }
}
