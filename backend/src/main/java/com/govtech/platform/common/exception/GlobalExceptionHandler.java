package com.govtech.platform.common.exception;

import com.govtech.platform.common.response.ErrorResponse;
import com.govtech.platform.common.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralised exception handler for all REST controllers.
 *
 * <p>Catches both custom application exceptions and common Spring MVC / Spring Security
 * exceptions, and maps them to a consistent {@link ErrorResponse} or
 * {@link ValidationErrorResponse} JSON structure.</p>
 *
 * <p><strong>Security note:</strong> stack traces, class names, and internal technical
 * details are never included in client-facing responses. The generic catch-all handler
 * logs the full exception server-side and returns a safe, generic 500 message.</p>
 *
 * <p><strong>Spring Security note:</strong> {@link AccessDeniedException} and
 * {@link AuthenticationException} are normally intercepted by Spring Security's filter
 * chain before reaching this advice (via {@code ExceptionTranslationFilter}).
 * These handlers act as a safety net for cases where the exceptions surface after the
 * filter chain (e.g. thrown inside a {@code @PostFilter} or method security check).</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Application-level exceptions ─────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler({InvalidStatusException.class, BusinessException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(
            RuntimeException ex, HttpServletRequest request) {
        log.warn("Bad request [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAction(
            UnauthorizedActionException ex, HttpServletRequest request) {
        log.warn("Unauthorized action [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage(), request);
    }

    // ── Bean Validation exceptions ────────────────────────────────────────────

    /**
     * Handles {@code @Valid} failures on {@code @RequestBody} parameters.
     * Returns a structured map of field → message pairs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            // Last writer wins: if a field has multiple violations, keep the first
            fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed [{}]: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message("Request validation failed")
                        .path(request.getRequestURI())
                        .fieldErrors(fieldErrors)
                        .build());
    }

    /**
     * Handles {@code @Validated} constraint violations on path/query variables and
     * service-level {@code @Validated} method parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String fieldName = violation.getPropertyPath().toString();
            // Strip method name prefix for cleaner output (e.g. "createCitizen.name" → "name")
            int lastDot = fieldName.lastIndexOf('.');
            String shortName = lastDot >= 0 ? fieldName.substring(lastDot + 1) : fieldName;
            fieldErrors.putIfAbsent(shortName, violation.getMessage());
        }
        log.warn("Constraint violation [{}]: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ValidationErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error("Validation Failed")
                        .message("Request validation failed")
                        .path(request.getRequestURI())
                        .fieldErrors(fieldErrors)
                        .build());
    }

    // ── Spring MVC exceptions ─────────────────────────────────────────────────

    /**
     * Handles malformed or unreadable JSON request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable request body [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Bad Request",
                "Request body is missing or malformed.", request);
    }

    /**
     * Handles type mismatches in path variables or request parameters
     * (e.g. passing a string where a Long ID is expected).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'.",
                ex.getValue(), ex.getName());
        log.warn("Type mismatch [{}]: {}", request.getRequestURI(), message);
        return error(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    /**
     * Handles unsupported HTTP method calls (e.g. DELETE on a read-only endpoint).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint.",
                ex.getMethod());
        log.warn("Method not supported [{}]: {}", request.getRequestURI(), message);
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", message, request);
    }

    // ── Spring Security exceptions ────────────────────────────────────────────

    /**
     * Safety net for {@link AccessDeniedException} that reaches this advice.
     * The primary handler is the {@code AccessDeniedHandler} on the Security filter chain.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to access this resource.", request);
    }

    /**
     * Specific handler for disabled/locked accounts — returns a user-friendly message
     * that the frontend can distinguish from a wrong-password 401.
     */
    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ErrorResponse> handleDisabledAccount(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Inactive account access attempt [{}]", request.getRequestURI());
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Your account has been deactivated. Please contact the administrator.", request);
    }

    /**
     * Safety net for {@link AuthenticationException} that reaches this advice.
     * The primary handler is the {@code AuthenticationEntryPoint} on the Security filter chain.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failure [{}]: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "Authentication is required to access this resource.", request);
    }

    // ── Generic catch-all ─────────────────────────────────────────────────────

    /**
     * Catches any unhandled exception. Logs the full stack trace server-side but
     * returns only a safe, generic message to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        // Full stack trace is logged — never included in the response
        log.error("Unexpected error [{}]", request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .timestamp(LocalDateTime.now())
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .path(request.getRequestURI())
                        .build());
    }
}
