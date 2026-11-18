package com.deckbuilder.mtgdeckbuilder.infrastructure.importer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Streams a Scryfall bulk JSONL file without loading it into memory.
 *
 * <p>
 * One JSON object per line is the published format, but the reader also
 * tolerates a UTF-8 BOM, blank lines, JSON array brackets and a trailing comma
 * on a line. Every line is parsed as an object; a malformed line fails loudly
 * so the importer can count the row as failed instead of silently dropping it.
 */
@Component
public class ScryfallBulkReader {

	private final ObjectMapper objectMapper;

	public ScryfallBulkReader(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public Stream<JsonNode> read(Path file) throws IOException {
		return lines(file).map(this::parse);
	}

	/**
	 * Returns the normalised, non-empty lines of the file without parsing them.
	 * The caller owns the stream and must close it.
	 */
	public Stream<String> lines(Path file) throws IOException {
		return Files.lines(file, StandardCharsets.UTF_8)
			.map(ScryfallBulkReader::normalize)
			.filter(line -> !line.isEmpty());
	}

	/**
	 * Parses one normalised line. Public so a caller can count a malformed line
	 * as a failed row and keep reading the rest of the file.
	 */
	public JsonNode parse(String line) {
		try {
			return objectMapper.readTree(line);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid JSON line: " + abbreviate(line), ex);
		}
	}

	private static String normalize(String rawLine) {
		String line = rawLine;
		if (line.startsWith("\uFEFF")) {
			line = line.substring(1);
		}
		line = line.trim();
		while (line.endsWith(",")) {
			line = line.substring(0, line.length() - 1).trim();
		}
		return line.equals("[") || line.equals("]") ? "" : line;
	}

	private static String abbreviate(String line) {
		return line.length() <= 120 ? line : line.substring(0, 120) + "...";
	}
}
