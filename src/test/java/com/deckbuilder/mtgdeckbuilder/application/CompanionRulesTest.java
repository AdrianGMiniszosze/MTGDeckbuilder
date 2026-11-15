package com.deckbuilder.mtgdeckbuilder.application;

import com.deckbuilder.mtgdeckbuilder.infrastructure.model.CardEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Companion Rules Tests")
class CompanionRulesTest {

    private static CardEntity card(String type, Integer cmc) {
        return card(type, cmc, null, null);
    }

    private static CardEntity card(String type, Integer cmc, List<String> subtypes, String manaCost) {
        final CardEntity card = new CardEntity();
        card.setCardType(type);
        card.setCmc(cmc);
        card.setSubtypes(subtypes);
        card.setManaCost(manaCost);
        return card;
    }

    @Test
    @DisplayName("Should ignore a null companion or card")
    void shouldIgnoreNulls() {
        assertThat(CompanionRules.validateByCompanionName(null, card("Creature", 5))).isNull();
        assertThat(CompanionRules.validateByCompanionName("lurrus", null)).isNull();
        assertThat(CompanionRules.validateByCompanionName("lurrus", card("Creature", null))).isNull();
    }

    @Test
    @DisplayName("Should ignore companions without a per-card rule")
    void shouldIgnoreUnknownCompanion() {
        assertThat(CompanionRules.validateByCompanionName("umori", card("Creature", 5))).isNull();
        assertThat(CompanionRules.validateByCompanionName("yorion", card("Creature", 5))).isNull();
    }

    @Test
    @DisplayName("Should detect permanents only")
    void shouldDetectPermanents() {
        assertThat(CompanionRules.isPermanent(card("Artifact", 1))).isTrue();
        assertThat(CompanionRules.isPermanent(card("Creature", 1))).isTrue();
        assertThat(CompanionRules.isPermanent(card("Enchantment", 1))).isTrue();
        assertThat(CompanionRules.isPermanent(card("Planeswalker", 1))).isTrue();
        assertThat(CompanionRules.isPermanent(card("Land", 0))).isTrue();
        assertThat(CompanionRules.isPermanent(card("Instant", 1))).isFalse();
        assertThat(CompanionRules.isPermanent(card(null, 1))).isFalse();
    }

    @Nested
    @DisplayName("Lurrus of the Dream-Den")
    class LurrusTests {

        @Test
        @DisplayName("Should reject permanents above mana value 2")
        void shouldRejectExpensivePermanent() {
            assertThat(CompanionRules.validateByCompanionName("Lurrus of the Dream-Den", card("Creature", 3)))
                    .isEqualTo("Companion Lurrus: permanents must have mana value 2 or less");
        }

        @Test
        @DisplayName("Should allow cheap permanents and expensive non permanents")
        void shouldAllowCheapPermanentAndSpell() {
            assertThat(CompanionRules.validateByCompanionName("lurrus", card("Creature", 2))).isNull();
            assertThat(CompanionRules.validateByCompanionName("lurrus", card("Instant", 6))).isNull();
        }
    }

    @Nested
    @DisplayName("Obosh, the Preypiercer")
    class OboshTests {

        @Test
        @DisplayName("Should reject even nonland mana values")
        void shouldRejectEvenSpell() {
            assertThat(CompanionRules.validateByCompanionName("obosh", card("Instant", 4)))
                    .isEqualTo("Companion Obosh: nonland cards must have odd mana value");
        }

        @Test
        @DisplayName("Should allow odd spells and even lands")
        void shouldAllowOddSpellAndLand() {
            assertThat(CompanionRules.validateByCompanionName("obosh", card("Instant", 3))).isNull();
            assertThat(CompanionRules.validateByCompanionName("obosh", card("Land", 4))).isNull();
        }
    }

    @Nested
    @DisplayName("Gyruda, Doom of Depths")
    class GyrudaTests {

        @Test
        @DisplayName("Should reject odd nonland mana values")
        void shouldRejectOddSpell() {
            assertThat(CompanionRules.validateByCompanionName("gyruda", card("Sorcery", 3)))
                    .isEqualTo("Companion Gyruda: nonland cards must have even mana value");
        }

        @Test
        @DisplayName("Should allow even spells and odd lands")
        void shouldAllowEvenSpellAndLand() {
            assertThat(CompanionRules.validateByCompanionName("gyruda", card("Sorcery", 2))).isNull();
            assertThat(CompanionRules.validateByCompanionName("gyruda", card("Land", 3))).isNull();
        }
    }

    @Nested
    @DisplayName("Keruga, the Macrosage")
    class KerugaTests {

        @Test
        @DisplayName("Should reject nonland cards below mana value 3")
        void shouldRejectCheapSpell() {
            assertThat(CompanionRules.validateByCompanionName("keruga", card("Instant", 2)))
                    .isEqualTo("Companion Keruga: nonland cards must have mana value 3 or greater");
        }

        @Test
        @DisplayName("Should allow expensive spells and cheap lands")
        void shouldAllowExpensiveSpellAndLand() {
            assertThat(CompanionRules.validateByCompanionName("keruga", card("Instant", 3))).isNull();
            assertThat(CompanionRules.validateByCompanionName("keruga", card("Land", 0))).isNull();
        }
    }

    @Nested
    @DisplayName("Kaheera, the Orphanguard")
    class KaheeraTests {

        @Test
        @DisplayName("Should allow listed creature subtypes")
        void shouldAllowListedSubtype() {
            assertThat(CompanionRules.validateByCompanionName("kaheera", card("Creature", 3, List.of("Cat"), null)))
                    .isNull();
            assertThat(CompanionRules.validateByCompanionName("kaheera",
                    card("Creature", 3, List.of("Elemental", "Beast"), null))).isNull();
        }

        @Test
        @DisplayName("Should reject creatures outside the listed subtypes")
        void shouldRejectUnlistedSubtype() {
            final String expected = "Companion Kaheera: creature cards must be Cat, Elemental, Nightmare, Dinosaur, or Beast";
            assertThat(CompanionRules.validateByCompanionName("kaheera", card("Creature", 3, List.of("Goblin"), null)))
                    .isEqualTo(expected);
            assertThat(CompanionRules.validateByCompanionName("kaheera", card("Creature", 3, null, null)))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("Should ignore non creatures")
        void shouldIgnoreNonCreature() {
            assertThat(CompanionRules.validateByCompanionName("kaheera", card("Instant", 3, List.of("Goblin"), null)))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Jegantha, the Wellspring")
    class JeganthaTests {

        @Test
        @DisplayName("Should reject repeated mana symbols")
        void shouldRejectRepeatedSymbol() {
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "{2}{R}{R}")))
                    .isEqualTo("Companion Jegantha: card mana cost contains multiple 'R' symbols");
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "{R/P}{R/P}")))
                    .contains("multiple 'R' symbols");
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "RR")))
                    .contains("multiple 'R' symbols");
        }

        @Test
        @DisplayName("Should allow distinct symbols, X, numerals and single hybrid symbols")
        void shouldAllowValidCosts() {
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "{1}{R}{G}")))
                    .isNull();
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "{X}{X}{X}")))
                    .isNull();
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "{2}{R/G}")))
                    .isNull();
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, "{C}{S}")))
                    .isNull();
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, ""))).isNull();
            assertThat(CompanionRules.validateByCompanionName("jegantha", card("Creature", 4, null, null))).isNull();
        }
    }
}
