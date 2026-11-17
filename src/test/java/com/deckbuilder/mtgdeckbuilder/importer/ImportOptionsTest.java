package com.deckbuilder.mtgdeckbuilder.importer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Import options")
class ImportOptionsTest {

	@TempDir
	Path tempDir;

	@Nested
	@DisplayName("Defaults")
	class Defaults {

		@Test
		@DisplayName("Should use 500 batch size and no limits")
		void shouldUseDefaults() {
			final ImportOptions options = new ImportOptions();

			assertThat(options.getBatchSize()).isEqualTo(500);
			assertThat(options.getLimit()).isNull();
			assertThat(options.getRulings()).isNull();
			assertThat(options.getFile()).isNull();
		}
	}

	@Nested
	@DisplayName("Validation")
	class Validation {

		@Test
		@DisplayName("Should accept a readable file")
		void shouldAcceptReadableFile() throws Exception {
			final Path file = Files.writeString(tempDir.resolve("cards.jsonl"), "{}");
			final ImportOptions options = options(file);

			assertThatCode(options::validate).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("Should accept an optional rulings file")
		void shouldAcceptRulingsFile() throws Exception {
			final Path file = Files.writeString(tempDir.resolve("cards.jsonl"), "{}");
			final Path rulings = Files.writeString(tempDir.resolve("rulings.jsonl"), "{}");
			final ImportOptions options = options(file);
			options.setRulings(rulings);

			assertThatCode(options::validate).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("Should reject a missing file option")
		void shouldRejectMissingFileOption() {
			assertThatThrownBy(() -> new ImportOptions().validate())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--import.file is required");
		}

		@Test
		@DisplayName("Should reject a file that does not exist")
		void shouldRejectMissingFile() {
			final ImportOptions options = options(tempDir.resolve("missing.jsonl"));

			assertThatThrownBy(options::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a readable file");
		}

		@Test
		@DisplayName("Should reject a directory as the input file")
		void shouldRejectDirectory() {
			final ImportOptions options = options(tempDir);

			assertThatThrownBy(options::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("not a readable file");
		}

		@Test
		@DisplayName("Should reject a missing rulings file")
		void shouldRejectMissingRulings() throws Exception {
			final Path file = Files.writeString(tempDir.resolve("cards.jsonl"), "{}");
			final ImportOptions options = options(file);
			options.setRulings(tempDir.resolve("missing-rulings.jsonl"));

			assertThatThrownBy(options::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--import.rulings");
		}

		@Test
		@DisplayName("Should reject a batch size below one")
		void shouldRejectSmallBatchSize() throws Exception {
			final ImportOptions options = options(Files.writeString(tempDir.resolve("cards.jsonl"), "{}"));
			options.setBatchSize(0);

			assertThatThrownBy(options::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--import.batch-size");
		}

		@Test
		@DisplayName("Should reject a batch size above the maximum")
		void shouldRejectLargeBatchSize() throws Exception {
			final ImportOptions options = options(Files.writeString(tempDir.resolve("cards.jsonl"), "{}"));
			options.setBatchSize(5001);

			assertThatThrownBy(options::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--import.batch-size");
		}

		@Test
		@DisplayName("Should accept a positive partial limit")
		void shouldAcceptPositiveLimit() throws Exception {
			final ImportOptions options = options(Files.writeString(tempDir.resolve("cards.jsonl"), "{}"));
			options.setLimit(10);

			assertThatCode(options::validate).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("Should reject a non-positive partial limit")
		void shouldRejectNonPositiveLimit() throws Exception {
			final ImportOptions options = options(Files.writeString(tempDir.resolve("cards.jsonl"), "{}"));
			options.setLimit(0);

			assertThatThrownBy(options::validate)
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("--import.limit");
		}
	}

	private static ImportOptions options(Path file) {
		final ImportOptions options = new ImportOptions();
		options.setFile(file);
		return options;
	}
}
