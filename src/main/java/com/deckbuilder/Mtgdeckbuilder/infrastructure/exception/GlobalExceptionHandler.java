package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for consistent error responses across the API
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Handle card not found exceptions
	 */
	@ExceptionHandler(CardNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCardNotFound(CardNotFoundException ex, HttpServletRequest request) {
		log.warn("Card not found: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("CARD_NOT_FOUND").status(HttpStatus.NOT_FOUND.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handle deck not found exceptions
	 */
	@ExceptionHandler(DeckNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleDeckNotFound(DeckNotFoundException ex, HttpServletRequest request) {
		log.warn("Deck not found: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("DECK_NOT_FOUND").status(HttpStatus.NOT_FOUND.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handle invalid deck composition exceptions
	 */
	@ExceptionHandler(InvalidDeckCompositionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidDeckComposition(InvalidDeckCompositionException ex,
			HttpServletRequest request) {
		log.warn("Invalid deck composition: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("INVALID_DECK_COMPOSITION").status(HttpStatus.BAD_REQUEST.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Handle duplicate card exceptions
	 */
	@ExceptionHandler(DuplicateCardException.class)
	public ResponseEntity<ErrorResponse> handleDuplicateCard(DuplicateCardException ex, HttpServletRequest request) {
		log.warn("Duplicate card violation: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("DUPLICATE_CARD_LIMIT_EXCEEDED").status(HttpStatus.BAD_REQUEST.value())
				.path(request.getRequestURI()).timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Handle format violation exceptions
	 */
	@ExceptionHandler(FormatViolationException.class)
	public ResponseEntity<ErrorResponse> handleFormatViolation(FormatViolationException ex,
			HttpServletRequest request) {
		log.warn("Format violation: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("FORMAT_VIOLATION").status(HttpStatus.BAD_REQUEST.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Handle user not found exceptions
	 */
	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
		log.warn("User not found: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("USER_NOT_FOUND").status(HttpStatus.NOT_FOUND.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handle tag not found exceptions
	 */
	@ExceptionHandler(TagNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleTagNotFound(TagNotFoundException ex, HttpServletRequest request) {
		log.warn("Tag not found: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("TAG_NOT_FOUND").status(HttpStatus.NOT_FOUND.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handle set not found exceptions
	 */
	@ExceptionHandler(SetNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleSetNotFound(SetNotFoundException ex, HttpServletRequest request) {
		log.warn("Set not found: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("SET_NOT_FOUND").status(HttpStatus.NOT_FOUND.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handle format not found exceptions
	 */
	@ExceptionHandler(FormatNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleFormatNotFound(FormatNotFoundException ex, HttpServletRequest request) {
		log.warn("Format not found: {}", ex.getMessage());

		final ErrorResponse error = ErrorResponse.builder().success(false).message(ex.getMessage())
				.error("FORMAT_NOT_FOUND").status(HttpStatus.NOT_FOUND.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	/**
	 * Handle validation errors from @Valid annotations
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		log.warn("Validation failed: {} errors", ex.getBindingResult().getErrorCount());

		final List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
		for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
			validationErrors.add(ErrorResponse.ValidationError.builder().field(error.getField())
					.message(error.getDefaultMessage()).rejectedValue(error.getRejectedValue()).build());
		}

		final ErrorResponse error = ErrorResponse.builder().success(false).message("Validation failed")
				.error("VALIDATION_ERROR").status(HttpStatus.BAD_REQUEST.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).validationErrors(validationErrors).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Handle constraint violation exceptions (database-level)
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		log.warn("Constraint violation: {}", ex.getMessage());

		final List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();
		ex.getConstraintViolations().forEach(violation -> {
			validationErrors.add(ErrorResponse.ValidationError.builder().field(violation.getPropertyPath().toString())
					.message(violation.getMessage()).rejectedValue(violation.getInvalidValue()).build());
		});

		final ErrorResponse error = ErrorResponse.builder().success(false).message("Constraint violation")
				.error("CONSTRAINT_VIOLATION").status(HttpStatus.BAD_REQUEST.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).validationErrors(validationErrors).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Handle data integrity violations (foreign key, unique constraints, etc.)
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.error("Data integrity violation", ex);

		String message = "Data integrity violation occurred";
		if (ex.getMessage() != null) {
			if (ex.getMessage().contains("duplicate key")) {
				message = "A record with this unique identifier already exists";
			} else if (ex.getMessage().contains("foreign key")) {
				message = "Referenced entity does not exist";
			}
		}

		final ErrorResponse error = ErrorResponse.builder().success(false).message(message)
				.error("DATA_INTEGRITY_VIOLATION").status(HttpStatus.CONFLICT.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	/**
	 * Handle method argument type mismatch (e.g., passing string where integer
	 * expected)
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		log.warn("Type mismatch: parameter '{}' expected type '{}'", ex.getName(), ex.getRequiredType());

		final String message = String.format("Invalid value for parameter '%s'. Expected type: %s", ex.getName(),
				ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

		final ErrorResponse error = ErrorResponse.builder().success(false).message(message)
				.error("INVALID_PARAMETER_TYPE").status(HttpStatus.BAD_REQUEST.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	/**
	 * Catch-all handler for unexpected exceptions
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error occurred", ex);

		final ErrorResponse error = ErrorResponse.builder().success(false)
				.message("An unexpected error occurred. Please try again later.").error("INTERNAL_SERVER_ERROR")
				.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).path(request.getRequestURI())
				.timestamp(LocalDateTime.now()).build();

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}
}
