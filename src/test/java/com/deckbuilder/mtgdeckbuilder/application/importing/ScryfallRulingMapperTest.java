package com.deckbuilder.mtgdeckbuilder.application.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Scryfall ruling mapper")
class ScryfallRulingMapperTest {

	private static final UUID ORACLE = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	@DisplayName("Should map a complete ruling and lower-case the source")
	void shouldMapRuling() throws Exception {
		final Optional<ImportRuling> ruling = ScryfallRulingMapper.toImportRuling(objectMapper.readTree("""
				{"oracle_id":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
				 "source":"WOTC","published_at":"2026-01-02","comment":"  A ruling.  "}
				"""));

		assertThat(ruling).contains(new ImportRuling(
				ORACLE, LocalDate.of(2026, 1, 2), "A ruling.", "wotc"));
	}

	@Test
	@DisplayName("Should attribute a missing source to Scryfall")
	void shouldDefaultSource() throws Exception {
		final Optional<ImportRuling> ruling = ScryfallRulingMapper.toImportRuling(objectMapper.readTree("""
				{"oracle_id":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
				 "published_at":"2026-01-02","comment":"A ruling."}
				"""));

		assertThat(ruling).get().extracting(ImportRuling::source).isEqualTo("scryfall");
	}

	@Test
	@DisplayName("Should skip a ruling without identity, date or comment")
	void shouldSkipIncompleteRulings() throws Exception {
		assertThat(ScryfallRulingMapper.toImportRuling(objectMapper.readTree(
				"{\"published_at\":\"2026-01-02\",\"comment\":\"x\"}"))).isEmpty();
		assertThat(ScryfallRulingMapper.toImportRuling(objectMapper.readTree(
				"{\"oracle_id\":\"not-a-uuid\",\"published_at\":\"2026-01-02\",\"comment\":\"x\"}")))
			.isEmpty();
		assertThat(ScryfallRulingMapper.toImportRuling(objectMapper.readTree(
				"{\"oracle_id\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\",\"published_at\":\"nope\",\"comment\":\"x\"}")))
			.isEmpty();
		assertThat(ScryfallRulingMapper.toImportRuling(objectMapper.readTree(
				"{\"oracle_id\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\",\"comment\":\"x\"}")))
			.isEmpty();
		assertThat(ScryfallRulingMapper.toImportRuling(objectMapper.readTree(
				"{\"oracle_id\":\"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa\",\"published_at\":\"2026-01-02\",\"comment\":\"  \"}")))
			.isEmpty();
		assertThat(ScryfallRulingMapper.toImportRuling(objectMapper.readTree("[]"))).isEmpty();
		assertThat(ScryfallRulingMapper.toImportRuling(null)).isEmpty();
	}
}
