package com.deckbuilder.mtgdeckbuilder.infrastructure.mapper;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardFaceEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Card Entity Mapper Tests")
class CardEntityMapperTest {

    private final CardEntityMapper mapper = Mappers.getMapper(CardEntityMapper.class);

    @Test
    @DisplayName("Should return null when importing a missing entity")
    void shouldReturnNullForMissingEntity() {
        assertThat(this.mapper.toImportModel(null, List.of())).isNull();
    }

    @Test
    @DisplayName("Should keep null faces when the import provides none")
    void shouldKeepNullFaces() {
        final CardEntity entity = new CardEntity();
        entity.setId(1L);
        entity.setName("Delver of Secrets");
        entity.setCmc(1);
        entity.setTypeLine("Creature");
        entity.setCardType("Creature");
        entity.setRarity("common");
        entity.setCardText("");

        assertThat(this.mapper.toImportModel(entity, null).getFaces()).isNull();
    }

    @Test
    @DisplayName("Should default a missing mana cost on a card face")
    void shouldDefaultFaceManaCost() {
        final CardFaceEntity face = new CardFaceEntity();

        this.mapper.applyFaceDefaults(face);

        assertThat(face.getManaCost()).isEmpty();
    }
}
