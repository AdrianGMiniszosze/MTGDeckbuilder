package com.deckbuilder.mtgdeckbuilder.infrastructure.exception;

import com.deckbuilder.mtgdeckbuilder.model.DeckIssue;
import com.deckbuilder.mtgdeckbuilder.model.DeckIssueCode;
import com.deckbuilder.mtgdeckbuilder.model.DeckValidationResult;
import com.deckbuilder.mtgdeckbuilder.model.IssueSeverity;
import com.deckbuilder.mtgdeckbuilder.model.ValidationStatus;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/test";

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static MockHttpServletRequest request() {
        return new MockHttpServletRequest("GET", PATH);
    }

    private static ErrorResponse body(ResponseEntity<ErrorResponse> response) {
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private static void assertMapped(ResponseEntity<ErrorResponse> response, HttpStatus status, String error) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        final ErrorResponse body = body(response);
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getError()).isEqualTo(error);
        assertThat(body.getStatus()).isEqualTo(status.value());
        assertThat(body.getPath()).isEqualTo(PATH);
        assertThat(body.getTimestamp()).isNotNull();
        assertThat(body.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("Should map card not found to 404")
    void shouldMapCardNotFound() {
        assertMapped(handler.handleCardNotFound(new CardNotFoundException(7L), request()), HttpStatus.NOT_FOUND,
                "CARD_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map deck not found to 404")
    void shouldMapDeckNotFound() {
        assertMapped(handler.handleDeckNotFound(new DeckNotFoundException(7L), request()), HttpStatus.NOT_FOUND,
                "DECK_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map invalid user data to 400")
    void shouldMapInvalidUserData() {
        assertMapped(handler.handleInvalidUserData(new InvalidUserDataException("hashed_password is required"),
                request()), HttpStatus.BAD_REQUEST, "INVALID_USER_DATA");
    }

    @Test
    @DisplayName("Should map invalid deck composition to 400")
    void shouldMapInvalidDeckComposition() {
        assertMapped(handler.handleInvalidDeckComposition(new InvalidDeckCompositionException("bad section"), request()),
                HttpStatus.BAD_REQUEST, "INVALID_DECK_COMPOSITION");
    }

    @Test
    @DisplayName("Should map duplicate card to 400")
    void shouldMapDuplicateCard() {
        assertMapped(handler.handleDuplicateCard(new DuplicateCardException("Lightning Bolt", 5, 4), request()),
                HttpStatus.BAD_REQUEST, "DUPLICATE_CARD_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Should map format violation to 400")
    void shouldMapFormatViolation() {
        assertMapped(handler.handleFormatViolation(new FormatViolationException("Lightning Bolt", "Standard", "banned"),
                request()), HttpStatus.BAD_REQUEST, "FORMAT_VIOLATION");
    }

    @Test
    @DisplayName("Should map user not found to 404")
    void shouldMapUserNotFound() {
        assertMapped(handler.handleUserNotFound(new UserNotFoundException(7L), request()), HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map tag not found to 404")
    void shouldMapTagNotFound() {
        assertMapped(handler.handleTagNotFound(new TagNotFoundException(7L), request()), HttpStatus.NOT_FOUND,
                "TAG_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map set not found to 404")
    void shouldMapSetNotFound() {
        assertMapped(handler.handleSetNotFound(new SetNotFoundException(7L), request()), HttpStatus.NOT_FOUND,
                "SET_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map format not found to 404")
    void shouldMapFormatNotFound() {
        assertMapped(handler.handleFormatNotFound(new FormatNotFoundException(7L), request()), HttpStatus.NOT_FOUND,
                "FORMAT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should map an illegal deck to 409 and expose its issues")
    void shouldMapDeckNotLegal() {
        // Given
        final DeckIssue issue = DeckIssue.builder().severity(IssueSeverity.ERROR).code(DeckIssueCode.DECK_TOO_SMALL)
                .message("Deck has 4 cards, minimum is 60").build();
        final DeckValidationResult validation = DeckValidationResult.builder().deckId(1L).issues(List.of(issue))
                .validationStatus(ValidationStatus.ILLEGAL).build();

        // When
        final ResponseEntity<ErrorResponse> response = handler.handleDeckNotLegal(
                new DeckNotLegalException(1L, validation), request());

        // Then
        assertMapped(response, HttpStatus.CONFLICT, "DECK_NOT_LEGAL");
        assertThat(body(response).getIssues()).containsExactly(issue);
    }

    @Test
    @DisplayName("Should map unreadable bodies to 400")
    void shouldMapUnreadableBody() {
        // Given
        final HttpMessageNotReadableException ex = new HttpMessageNotReadableException("broken json",
                new MockHttpInputMessage(new byte[0]));

        // When
        final ResponseEntity<ErrorResponse> response = handler.handleUnreadableBody(ex, request());

        // Then
        assertMapped(response, HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY");
        assertThat(body(response).getMessage()).isEqualTo("Request body is missing or invalid");
    }

    @Test
    @DisplayName("Should map bean validation errors to 400 with field details")
    void shouldMapValidationErrors() {
        // Given
        final BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "card");
        binding.addError(new FieldError("card", "name", null, false, null, null, "must not be null"));
        final MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        // When
        final ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex, request());

        // Then
        assertMapped(response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        assertThat(body(response).getValidationErrors()).hasSize(1);
        assertThat(body(response).getValidationErrors().get(0).getField()).isEqualTo("name");
        assertThat(body(response).getValidationErrors().get(0).getMessage()).isEqualTo("must not be null");
    }

    @Test
    @DisplayName("Should map constraint violations to 400 with field details")
    void shouldMapConstraintViolation() {
        // Given
        final Set<jakarta.validation.ConstraintViolation<Sample>> violations = VALIDATOR
                .validate(new Sample(null));
        final ConstraintViolationException ex = new ConstraintViolationException(violations);

        // When
        final ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex, request());

        // Then
        assertMapped(response, HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION");
        assertThat(body(response).getValidationErrors()).hasSize(1);
        assertThat(body(response).getValidationErrors().get(0).getField()).isEqualTo("name");
    }

    @Test
    @DisplayName("Should describe duplicate keys as an existing record")
    void shouldExplainDuplicateKey() {
        // When
        final ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"), request());

        // Then
        assertMapped(response, HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION");
        assertThat(body(response).getMessage()).isEqualTo("A record with this unique identifier already exists");
    }

    @Test
    @DisplayName("Should describe foreign keys as a missing reference")
    void shouldExplainForeignKey() {
        // When
        final ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("violates foreign key constraint"), request());

        // Then
        assertThat(body(response).getMessage()).isEqualTo("Referenced entity does not exist");
    }

    @Test
    @DisplayName("Should fall back to a generic message when the cause is unknown")
    void shouldFallBackToGenericIntegrityMessage() {
        // When
        final ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("something else"), request());

        // Then
        assertThat(body(response).getMessage()).isEqualTo("Data integrity violation occurred");
    }

    @Test
    @DisplayName("Should keep the generic message when the exception has no message")
    void shouldHandleMissingIntegrityMessage() {
        // When
        final ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException((String) null), request());

        // Then
        assertMapped(response, HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION");
        assertThat(body(response).getMessage()).isEqualTo("Data integrity violation occurred");
    }

    @Test
    @DisplayName("Should name the parameter and its expected type on a mismatch")
    void shouldMapTypeMismatch() {
        // Given
        final MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", Integer.class,
                "id", null, new NumberFormatException("abc"));

        // When
        final ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request());

        // Then
        assertMapped(response, HttpStatus.BAD_REQUEST, "INVALID_PARAMETER_TYPE");
        assertThat(body(response).getMessage())
                .isEqualTo("Invalid value for parameter 'id'. Expected type: Integer");
    }

    @Test
    @DisplayName("Should say unknown when the expected type is not available")
    void shouldMapTypeMismatchWithoutType() {
        // Given
        final MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("abc", null, "id", null,
                new NumberFormatException("abc"));

        // When
        final ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request());

        // Then
        assertThat(body(response).getMessage()).isEqualTo("Invalid value for parameter 'id'. Expected type: unknown");
    }

    @Test
    @DisplayName("Should hide unexpected exception details behind a 500")
    void shouldMapGenericException() {
        // When
        final ResponseEntity<ErrorResponse> response = handler.handleGenericException(
                new IllegalStateException("database exploded"), request());

        // Then
        assertMapped(response, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
        assertThat(body(response).getMessage()).doesNotContain("database exploded");
    }

    private record Sample(@NotNull String name) {
    }
}
