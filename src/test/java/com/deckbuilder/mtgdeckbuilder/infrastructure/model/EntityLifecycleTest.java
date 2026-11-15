package com.deckbuilder.mtgdeckbuilder.infrastructure.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JPA lifecycle callbacks fill the audit columns before insert/update; the
 * defaults must not overwrite a value the caller already set.
 */
@DisplayName("Entity Lifecycle Tests")
class EntityLifecycleTest {

    @Test
    @DisplayName("Should fill both user timestamps on insert")
    void shouldFillUserTimestampsOnInsert() {
        final UserEntity user = new UserEntity();

        user.onCreate();

        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should keep the user creation date when it is already set")
    void shouldKeepUserCreatedAt() {
        final UserEntity user = new UserEntity();
        final LocalDateTime original = LocalDateTime.of(2020, 1, 1, 10, 0);
        user.setCreatedAt(original);

        user.onCreate();

        assertThat(user.getCreatedAt()).isEqualTo(original);
        assertThat(user.getUpdatedAt()).isAfter(original);
    }

    @Test
    @DisplayName("Should refresh the user update timestamp")
    void shouldRefreshUserUpdatedAt() {
        final UserEntity user = new UserEntity();
        final LocalDateTime original = LocalDateTime.of(2020, 1, 1, 10, 0);
        user.setUpdatedAt(original);

        user.onUpdate();

        assertThat(user.getUpdatedAt()).isAfter(original);
    }

    @Test
    @DisplayName("Should fill the card tag creation date on insert")
    void shouldFillCardTagCreatedAt() {
        final CardTagEntity tag = CardTagEntity.builder().cardId(1L).tagId(2L).weight(1.0).confidence(1.0)
                .source("test").build();

        tag.onCreate();

        assertThat(tag.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should keep the card tag creation date when it is already set")
    void shouldKeepCardTagCreatedAt() {
        final CardTagEntity tag = CardTagEntity.builder().cardId(1L).tagId(2L).weight(1.0).confidence(1.0)
                .source("test").build();
        final LocalDateTime original = LocalDateTime.of(2020, 1, 1, 10, 0);
        tag.setCreatedAt(original);

        tag.onCreate();

        assertThat(tag.getCreatedAt()).isEqualTo(original);
    }
}
