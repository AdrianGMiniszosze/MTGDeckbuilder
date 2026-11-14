package com.deckbuilder.mtgdeckbuilder.infrastructure;

import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.CardEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CardEntityMapperTest {

    private final CardEntityMapper mapper = Mappers.getMapper(CardEntityMapper.class);

    @Test
    void mapsScryfallIdentityAndTimestampsFromEntityToModel() {
        UUID scryfallId = UUID.randomUUID();
        UUID oracleId = UUID.randomUUID();
        OffsetDateTime importedAt = OffsetDateTime.parse("2026-09-27T10:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-09-28T10:00:00Z");
        CardEntity entity = new CardEntity();
        entity.setScryfallId(scryfallId);
        entity.setOracleId(oracleId);
        entity.setImportedAt(importedAt);
        entity.setUpdatedAt(updatedAt);

        Card model = mapper.toModel(entity);

        assertThat(model.getScryfallId()).isEqualTo(scryfallId);
        assertThat(model.getOracleId()).isEqualTo(oracleId);
        assertThat(model.getImportedAt()).isEqualTo(importedAt);
        assertThat(model.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsScryfallIdentityAndTimestampsFromModelToEntity() {
        UUID scryfallId = UUID.randomUUID();
        UUID oracleId = UUID.randomUUID();
        OffsetDateTime importedAt = OffsetDateTime.parse("2026-09-27T10:00:00Z");
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-09-28T10:00:00Z");
        Card model = Card.builder()
                .scryfallId(scryfallId)
                .oracleId(oracleId)
                .importedAt(importedAt)
                .updatedAt(updatedAt)
                .build();

        CardEntity entity = mapper.toEntity(model);

        assertThat(entity.getScryfallId()).isEqualTo(scryfallId);
        assertThat(entity.getOracleId()).isEqualTo(oracleId);
        assertThat(entity.getImportedAt()).isEqualTo(importedAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
