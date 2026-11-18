package com.deckbuilder.mtgdeckbuilder.importer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Command-line options for the {@code import} profile.
 *
 * <p>
 * Defaults follow D-025 and D-031: batches of 500 and no partial limit. The
 * limit exists for development fixtures and never marks cards as obsolete,
 * because a partial load has not seen the whole catalogue.
 */
@Component
@Profile("import")
@ConfigurationProperties(prefix = "import")
public class ImportOptions {

	public static final int DEFAULT_BATCH_SIZE = 500;
	public static final int MAX_BATCH_SIZE = 5000;

	private Path file;
	private Path rulings;
	private int batchSize = DEFAULT_BATCH_SIZE;
	private Integer limit;

	public Path getFile() {
		return file;
	}

	public void setFile(Path file) {
		this.file = file;
	}

	public Path getRulings() {
		return rulings;
	}

	public void setRulings(Path rulings) {
		this.rulings = rulings;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public Integer getLimit() {
		return limit;
	}

	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	/**
	 * Fails with the same exit code 2 path as missing configuration.
	 */
	public void validate() {
		if (file == null) {
			throw new IllegalArgumentException("--import.file is required");
		}
		if (!Files.isRegularFile(file)) {
			throw new IllegalArgumentException("--import.file is not a readable file: " + file);
		}
		if (rulings != null && !Files.isRegularFile(rulings)) {
			throw new IllegalArgumentException("--import.rulings is not a readable file: " + rulings);
		}
		if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
			throw new IllegalArgumentException(
					"--import.batch-size must be between 1 and " + MAX_BATCH_SIZE);
		}
		if (limit != null && limit < 1) {
			throw new IllegalArgumentException("--import.limit must be a positive number");
		}
	}
}
