package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard error response format for API errors
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

	private boolean success;
	private String message;
	private String error;
	private int status;
	private String path;
	private LocalDateTime timestamp;
	private List<ValidationError> validationErrors;

	/**
	 * Validation error details for field-level errors
	 */
	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ValidationError {
		private String field;
		private String message;
		private Object rejectedValue;
	}
}
