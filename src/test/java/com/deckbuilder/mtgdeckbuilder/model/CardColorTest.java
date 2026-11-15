package com.deckbuilder.mtgdeckbuilder.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Card Color Tests")
class CardColorTest {

    @Test
    @DisplayName("Should expose the code and full name")
    void shouldExposeCodeAndName() {
        assertThat(CardColor.W.getCode()).isEqualTo("W");
        assertThat(CardColor.U.getFullName()).isEqualTo("Blue");
    }

    @ParameterizedTest
    @ValueSource(strings = { "r", "R", "red", "RED" })
    @DisplayName("Should parse codes and names ignoring case")
    void shouldParseCodesAndNames(String value) {
        assertThat(CardColor.fromString(value)).isEqualTo(CardColor.R);
    }

    @Test
    @DisplayName("Should return null for missing values")
    void shouldReturnNullForMissingValues() {
        assertThat(CardColor.fromString(null)).isNull();
        assertThat(CardColor.fromString("")).isNull();
    }

    @Test
    @DisplayName("Should reject unknown colors")
    void shouldRejectUnknownColors() {
        assertThatThrownBy(() -> CardColor.fromString("purple"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown MTG color: purple");
    }
}
