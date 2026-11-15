package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Domain Exception Tests")
class DomainExceptionTest {

    @Test
    @DisplayName("Should describe a missing card by name")
    void shouldDescribeCardByName() {
        final CardNotFoundException ex = new CardNotFoundException("Lightning Bolt");

        assertThat(ex).hasMessage("Card not found with name: Lightning Bolt");
    }

    @Test
    @DisplayName("Should describe a missing deck by name and owner")
    void shouldDescribeDeckByNameAndUser() {
        final DeckNotFoundException ex = new DeckNotFoundException("Burn", 7L);

        assertThat(ex).hasMessage("Deck not found with name: Burn for user: 7");
    }

    @Test
    @DisplayName("Should describe a missing format by name or by cause")
    void shouldDescribeFormat() {
        final IllegalStateException cause = new IllegalStateException("boom");

        assertThat(new FormatNotFoundException("Modern")).hasMessage("Format not found with name: Modern");
        assertThat(new FormatNotFoundException("wrapped", cause)).hasMessage("wrapped").hasCause(cause);
    }

    @Test
    @DisplayName("Should describe a missing set by name or by cause")
    void shouldDescribeSet() {
        final IllegalStateException cause = new IllegalStateException("boom");

        assertThat(new SetNotFoundException("Dominaria")).hasMessage("Set not found with name: Dominaria");
        assertThat(new SetNotFoundException("wrapped", cause)).hasMessage("wrapped").hasCause(cause);
    }

    @Test
    @DisplayName("Should describe a missing tag by name or by cause")
    void shouldDescribeTag() {
        final IllegalStateException cause = new IllegalStateException("boom");

        assertThat(new TagNotFoundException("ramp")).hasMessage("Tag not found with name: ramp");
        assertThat(new TagNotFoundException("wrapped", cause)).hasMessage("wrapped").hasCause(cause);
    }

    @Test
    @DisplayName("Should describe a missing user by username or by cause")
    void shouldDescribeUser() {
        final IllegalStateException cause = new IllegalStateException("boom");

        assertThat(new UserNotFoundException("ada")).hasMessage("User not found with username: ada");
        assertThat(new UserNotFoundException("wrapped", cause)).hasMessage("wrapped").hasCause(cause);
    }
}
