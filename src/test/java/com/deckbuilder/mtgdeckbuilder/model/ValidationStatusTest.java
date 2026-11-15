package com.deckbuilder.mtgdeckbuilder.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Validation Status Tests")
class ValidationStatusTest {

    @Test
    @DisplayName("Should expose the API value of each status")
    void shouldExposeApiValues() {
        assertThat(ValidationStatus.LEGAL.getValue()).isEqualTo("legal");
        assertThat(ValidationStatus.ILLEGAL.getValue()).isEqualTo("illegal");
        assertThat(ValidationStatus.UNKNOWN.getValue()).isEqualTo("unknown");
    }
}
