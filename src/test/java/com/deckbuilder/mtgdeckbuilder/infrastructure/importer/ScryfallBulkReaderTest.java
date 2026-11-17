package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Scryfall bulk reader")
class ScryfallBulkReaderTest {

	private final ScryfallBulkReader reader = new ScryfallBulkReader(new ObjectMapper());

	@TempDir
	Path tempDir;

	@Test
	@DisplayName("Should read BOM, blank lines, brackets and a trailing comma")
	void shouldReadWrapperAndCleanLines() throws Exception {
		final Path file = tempDir.resolve("cards.jsonl");
		Files.writeString(file, "\uFEFF[\n\n{\"id\":\"a\"},\n{\"id\":\"b\"}\n]\n");

		final List<JsonNode> cards = read(file);

		assertThat(cards).hasSize(2);
		assertThat(cards.get(0).get("id").asText()).isEqualTo("a");
		assertThat(cards.get(1).get("id").asText()).isEqualTo("b");
	}

	@Test
	@DisplayName("Should strip several trailing commas")
	void shouldStripSeveralTrailingCommas() throws Exception {
		final Path file = tempDir.resolve("commas.jsonl");
		Files.writeString(file, "{\"id\":\"a\"},,,\n");

		final List<JsonNode> cards = read(file);

		assertThat(cards).hasSize(1);
	}

	@Test
	@DisplayName("Should fail on a malformed line")
	void shouldFailOnMalformedLine() throws Exception {
		final Path file = tempDir.resolve("broken.jsonl");
		Files.writeString(file, "{\"id\":\n");

		assertThatThrownBy(() -> read(file))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("Invalid JSON line");
	}

	@Test
	@DisplayName("Should abbreviate a long malformed line in the error")
	void shouldAbbreviateLongMalformedLine() throws Exception {
		final Path file = tempDir.resolve("long-broken.jsonl");
		Files.writeString(file, "{".repeat(200));

		assertThatThrownBy(() -> read(file))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageEndingWith("...");
	}

	@Test
	@DisplayName("Should fail when the file does not exist")
	void shouldFailWhenFileIsMissing() {
		final Path missing = tempDir.resolve("missing.jsonl");

		assertThatThrownBy(() -> reader.read(missing)).isInstanceOf(IOException.class);
	}

	private List<JsonNode> read(Path file) throws IOException {
		try (Stream<JsonNode> stream = reader.read(file)) {
			return stream.toList();
		}
	}
}
