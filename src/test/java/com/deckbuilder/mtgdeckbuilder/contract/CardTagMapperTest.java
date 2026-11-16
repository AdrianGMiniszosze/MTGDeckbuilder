package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.CardTagDTO;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.CardTagMapper;
import com.deckbuilder.mtgdeckbuilder.model.CardTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Database tag sources are lowercase while the generated enum constants are
 * uppercase, so the mapper must not rely on {@code Enum.valueOf}.
 */
@DisplayName("Card Tag Mapper Tests")
class CardTagMapperTest {

	private final CardTagMapper mapper = Mappers.getMapper(CardTagMapper.class);

	@Test
	@DisplayName("Should map the lowercase source stored in the database")
	void shouldMapLowercaseSource() {
		final CardTagDTO dto = this.mapper.toCardTagDTO(tag("ai_text"));

		assertThat(dto.getSource()).isEqualTo(CardTagDTO.Source.AI_TEXT);
	}

	@Test
	@DisplayName("Should ignore case and surrounding spaces")
	void shouldIgnoreCaseAndSpaces() {
		final CardTagDTO dto = this.mapper.toCardTagDTO(tag(" MANUAL "));

		assertThat(dto.getSource()).isEqualTo(CardTagDTO.Source.MANUAL);
	}

	@Test
	@DisplayName("Should keep a null source")
	void shouldKeepNullSource() {
		assertThat(this.mapper.sourceToDto(null)).isNull();
	}

	@Test
	@DisplayName("Should degrade an unknown source to heuristic")
	void shouldDegradeUnknownSource() {
		assertThat(this.mapper.toCardTagDTO(tag("scryfall_import")).getSource())
			.isEqualTo(CardTagDTO.Source.HEURISTIC);
	}

	@Test
	@DisplayName("Should store the lowercase enum value in the database")
	void shouldStoreLowercaseSource() {
		final CardTag tag = this.mapper.toCardTag(dto(CardTagDTO.Source.ADMIN));

		assertThat(tag.getSource()).isEqualTo("admin");
	}

	@Test
	@DisplayName("Should map a null source back to null")
	void shouldMapNullSourceBackToNull() {
		assertThat(this.mapper.sourceToString(null)).isNull();
	}

	private static CardTag tag(String source) {
		return CardTag.builder().cardId(1L).tagId(2L).weight(1.0).confidence(1.0).source(source).build();
	}

	private static CardTagDTO dto(CardTagDTO.Source source) {
		return CardTagDTO.builder()
			.card_id(1)
			.tag_id(2)
			.weight(1.0f)
			.confidence(1.0f)
			.source(source)
			.build();
	}
}
