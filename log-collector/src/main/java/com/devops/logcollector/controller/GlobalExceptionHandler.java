package com.devops.logcollector.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for REST layer errors.
 *
 * <ul>
 *   <li>Validation failures  → 400 with field-level detail</li>
 *   <li>Malformed JSON       → 400 with descriptive message</li>
 *   <li>Unexpected errors    → 500</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation (@Valid) failures */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<String> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(FieldError::getDefaultMessage)
                                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST, "Validation failed", errors);

        return ResponseEntity.badRequest().body(body);
    }

    /** Malformed JSON / type-mismatch (e.g. wrong timestamp format) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(
            HttpMessageNotReadableException ex) {

        log.warn("Malformed request body: {}", ex.getMessage());

        Map<String, Object> body = buildErrorBody(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON or incorrect field type: " + ex.getMostSpecificCause().getMessage(),
                List.of());

        return ResponseEntity.badRequest().body(body);
    }

    /** Catch-all */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        Map<String, Object> body = buildErrorBody(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private Map<String, Object> buildErrorBody(HttpStatus status, String message,
                                               List<String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    status.value());
        body.put("error",     status.getReasonPhrase());
        body.put("message",   message);
        if (!errors.isEmpty()) body.put("details", errors);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
