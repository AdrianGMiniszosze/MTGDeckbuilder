package com.deckbuilder.mtgdeckbuilder.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pagination Config Tests")
class PaginationConfigTest {

    private final PaginationConfig config = new PaginationConfig();

    @Test
    @DisplayName("Should expose the documented defaults")
    void shouldExposeDefaults() {
        assertThat(this.config.getDefaultPageSize()).isEqualTo(20);
        assertThat(this.config.getMaxPageSize()).isEqualTo(100);
        assertThat(this.config.getMinPageSize()).isEqualTo(1);
        assertThat(this.config.getDefaultPageNumber()).isZero();
    }

    @Test
    @DisplayName("Should use the default page size when none is requested")
    void shouldUseDefaultPageSize() {
        assertThat(this.config.validatePageSize(null)).isEqualTo(20);
    }

    @Test
    @DisplayName("Should clamp the page size to the configured range")
    void shouldClampPageSize() {
        assertThat(this.config.validatePageSize(0)).isEqualTo(1);
        assertThat(this.config.validatePageSize(-7)).isEqualTo(1);
        assertThat(this.config.validatePageSize(42)).isEqualTo(42);
        assertThat(this.config.validatePageSize(101)).isEqualTo(100);
        assertThat(this.config.validatePageSize(Integer.MAX_VALUE)).isEqualTo(100);
    }

    @Test
    @DisplayName("Should reject negative page numbers and keep the rest")
    void shouldValidatePageNumber() {
        assertThat(this.config.validatePageNumber(null)).isZero();
        assertThat(this.config.validatePageNumber(-3)).isZero();
        assertThat(this.config.validatePageNumber(0)).isZero();
        assertThat(this.config.validatePageNumber(5)).isEqualTo(5);
    }

    @Test
    @DisplayName("Should honour overridden bounds")
    void shouldHonourOverriddenBounds() {
        this.config.setDefaultPageSize(5);
        this.config.setMinPageSize(2);
        this.config.setMaxPageSize(10);
        this.config.setDefaultPageNumber(3);

        assertThat(this.config.validatePageSize(null)).isEqualTo(5);
        assertThat(this.config.validatePageSize(1)).isEqualTo(2);
        assertThat(this.config.validatePageSize(50)).isEqualTo(10);
        assertThat(this.config.validatePageNumber(null)).isEqualTo(3);
    }
}
