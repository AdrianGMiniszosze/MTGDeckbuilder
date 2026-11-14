package com.deckbuilder.mtgdeckbuilder.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for pagination across the application
 */
@Configuration
@ConfigurationProperties(prefix = "app.pagination")
@Data
public class PaginationConfig {

	/**
	 * Default page size when not specified by client
	 */
	private int defaultPageSize = 20;

	/**
	 * Maximum allowed page size to prevent memory issues
	 */
	private int maxPageSize = 100;

	/**
	 * Minimum page size
	 */
	private int minPageSize = 1;

	/**
	 * Default page number (0-indexed)
	 */
	private int defaultPageNumber = 0;

	/**
	 * Validates and normalizes a page size parameter
	 *
	 * @param pageSize
	 *            the requested page size (can be null)
	 * @return validated page size within bounds
	 */
	public int validatePageSize(Integer pageSize) {
		if (pageSize == null) {
			return this.defaultPageSize;
		}
		return Math.min(Math.max(pageSize, this.minPageSize), this.maxPageSize);
	}

	/**
	 * Validates and normalizes a page number parameter
	 *
	 * @param pageNumber
	 *            the requested page number (can be null)
	 * @return validated page number (non-negative)
	 */
	public int validatePageNumber(Integer pageNumber) {
		if (pageNumber == null) {
			return this.defaultPageNumber;
		}
		return Math.max(pageNumber, 0);
	}
}
