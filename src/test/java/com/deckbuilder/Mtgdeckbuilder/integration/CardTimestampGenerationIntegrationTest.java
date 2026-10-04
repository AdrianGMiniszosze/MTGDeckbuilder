package com.deckbuilder.mtgdeckbuilder.integration;

import com.deckbuilder.mtgdeckbuilder.application.CardService;
import com.deckbuilder.mtgdeckbuilder.model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CardTimestampGenerationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void configureDatabaseGeneratedTimestamps() {
        jdbcTemplate.execute("ALTER TABLE cards ALTER COLUMN imported_at SET DEFAULT CURRENT_TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE cards ALTER COLUMN updated_at SET DEFAULT CURRENT_TIMESTAMP");
        jdbcTemplate.execute("""
                CREATE OR REPLACE FUNCTION set_cards_updated_at()
                RETURNS TRIGGER AS $$
                BEGIN
                    NEW.updated_at = OLD.updated_at + INTERVAL '1 second';
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql
                """);
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS trg_cards_updated_at ON cards");
        jdbcTemplate.execute("""
                CREATE TRIGGER trg_cards_updated_at
                BEFORE UPDATE ON cards
                FOR EACH ROW
                EXECUTE FUNCTION set_cards_updated_at()
                """);
    }

    @Test
    void returnsDatabaseGeneratedTimestampsAfterCreateAndUpdate() {
        UUID scryfallId = UUID.randomUUID();
        UUID oracleId = UUID.randomUUID();
        Card created = cardService.createCard(Card.builder()
                .name("Generated Timestamp Test")
                .manaCost("{1}{G}")
                .cmc(2)
                .typeLine("Creature — Test")
                .cardType("Creature")
                .rarity("common")
                .cardText("Test text")
                .imageUrl("https://example.test/card")
                .language("en")
                .foil(false)
                .gameChanger(false)
                .unlimitedCopies(false)
                .scryfallId(scryfallId)
                .oracleId(oracleId)
                .build());

        assertThat(created.getImportedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();

        Card updated = cardService.updateCard(created.getId(), Card.builder()
                .name("Generated Timestamp Test Updated")
                .manaCost("{1}{G}")
                .cmc(2)
                .typeLine("Creature — Test")
                .cardType("Creature")
                .rarity("common")
                .cardText("Updated test text")
                .imageUrl("https://example.test/card")
                .language("en")
                .foil(false)
                .gameChanger(false)
                .unlimitedCopies(false)
                .build()).orElseThrow();

        assertThat(updated.getScryfallId()).isEqualTo(scryfallId);
        assertThat(updated.getOracleId()).isEqualTo(oracleId);
        assertThat(updated.getImportedAt()).isEqualTo(created.getImportedAt());
        assertThat(updated.getUpdatedAt()).isAfter(created.getUpdatedAt());
        assertThat(updated.getUpdatedAt()).isEqualTo(readUpdatedAt(created.getId()));
        assertThat(updated.getUpdatedAt()).isNotNull();
    }

    private OffsetDateTime readUpdatedAt(Long cardId) {
        return jdbcTemplate.queryForObject(
                "SELECT updated_at FROM cards WHERE id = ?",
                (resultSet, rowNumber) -> resultSet.getObject(1, OffsetDateTime.class),
                cardId);
    }
}




