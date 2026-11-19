package com.deckbuilder.mtgdeckbuilder.contract;

import com.deckbuilder.apigenerator.openapi.api.model.FormatDTO;
import com.deckbuilder.mtgdeckbuilder.contract.mapper.FormatMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.mapper.FormatEntityMapper;
import com.deckbuilder.mtgdeckbuilder.infrastructure.model.FormatEntity;
import com.deckbuilder.mtgdeckbuilder.model.Format;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Format Mapper Tests")
class FormatMapperTest {

	private final FormatMapper formatMapper = Mappers.getMapper(FormatMapper.class);

	private final FormatEntityMapper formatEntityMapper = Mappers.getMapper(FormatEntityMapper.class);

	@Test
	@DisplayName("Should map every format rule between DTO and domain")
	void shouldMapAllRules() {
		// Given
		final FormatDTO dto = FormatDTO.builder().id(4).format_name("Commander").description("100 cards")
			.min_deck_size(100).max_deck_size(100).max_sideboard_size(0).max_copies(1).singleton(true)
			.requires_commander(true).tracks_legality(true).build();

		// When
		final Format model = this.formatMapper.toModel(dto);

		// Then
		assertThat(model.getName()).isEqualTo("Commander");
		assertThat(model.getMinDeckSize()).isEqualTo(100);
		assertThat(model.getMaxDeckSize()).isEqualTo(100);
		assertThat(model.getMaxSideboardSize()).isZero();
		assertThat(model.getMaxCopies()).isEqualTo(1);
		assertThat(model.getSingleton()).isTrue();
		assertThat(model.getRequiresCommander()).isTrue();
		assertThat(model.getTracksLegality()).isTrue();
	}

	@Test
	@DisplayName("Should derive deck_size from max_deck_size when present")
	void shouldDeriveDeckSizeFromMax() {
		final Format model = Format.builder().name("Commander").minDeckSize(100).maxDeckSize(100).build();

		assertThat(this.formatMapper.toDto(model).getDeck_size()).isEqualTo(100);
	}

	@Test
	@DisplayName("Should fall back to min_deck_size for the derived deck_size")
	void shouldDeriveDeckSizeFromMin() {
		final Format model = Format.builder().name("Casual").minDeckSize(40).build();

		assertThat(this.formatMapper.toDto(model).getDeck_size()).isEqualTo(40);
	}

	@Test
	@DisplayName("Should apply creation defaults when a rule is omitted")
	void shouldApplyCreationDefaults() {
		// When
		final FormatEntity entity = this.formatEntityMapper.toEntity(Format.builder().name("Modern").build());

		// Then
		assertThat(entity.getMinDeckSize()).isEqualTo(60);
		assertThat(entity.getMaxDeckSize()).isNull();
		assertThat(entity.getMaxCopies()).isEqualTo(4);
		assertThat(entity.isSingleton()).isFalse();
		assertThat(entity.isRequiresCommander()).isFalse();
		assertThat(entity.isTracksLegality()).isTrue();
	}

	@Test
	@DisplayName("Should keep stored rules when the update omits them")
	void shouldKeepRulesOnPartialUpdate() {
		// Given
		final FormatEntity entity = FormatEntity.builder().id(2L).name("Commander").minDeckSize(100)
			.maxDeckSize(100).maxSideboardSize(0).maxCopies(1).singleton(true).requiresCommander(true)
			.tracksLegality(true).build();

		// When
		this.formatEntityMapper.updateEntity(Format.builder().description("Edited text").build(), entity);

		// Then
		assertThat(entity.getDescription()).isEqualTo("Edited text");
		assertThat(entity.getName()).isEqualTo("Commander");
		assertThat(entity.getMinDeckSize()).isEqualTo(100);
		assertThat(entity.getMaxDeckSize()).isEqualTo(100);
		assertThat(entity.getMaxCopies()).isEqualTo(1);
		assertThat(entity.isSingleton()).isTrue();
		assertThat(entity.isRequiresCommander()).isTrue();
	}

	@Test
	@DisplayName("Should overwrite the rules provided in an update")
	void shouldOverwriteProvidedRules() {
		// Given
		final FormatEntity entity = FormatEntity.builder().name("Casual").maxCopies(4).singleton(false).build();

		// When
		this.formatEntityMapper.updateEntity(Format.builder().maxCopies(1).singleton(true).tracksLegality(false).build(),
				entity);

		// Then
		assertThat(entity.getName()).isEqualTo("Casual");
		assertThat(entity.getMaxCopies()).isEqualTo(1);
		assertThat(entity.isSingleton()).isTrue();
		assertThat(entity.isTracksLegality()).isFalse();
	}

	@Test
	@DisplayName("Should map a format entity back to the domain")
	void shouldMapEntityToModel() {
		final FormatEntity entity = FormatEntity.builder().id(3L).name("Legacy").minDeckSize(60).maxCopies(4)
			.tracksLegality(true).build();

		final Format model = this.formatEntityMapper.toModel(entity);

		assertThat(model.getId()).isEqualTo(3L);
		assertThat(model.getName()).isEqualTo("Legacy");
		assertThat(model.getMinDeckSize()).isEqualTo(60);
		assertThat(model.getMaxCopies()).isEqualTo(4);
	}
}
