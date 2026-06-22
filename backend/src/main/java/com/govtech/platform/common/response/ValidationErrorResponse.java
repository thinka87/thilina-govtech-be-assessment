package com.govtech.platform.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Error response body returned specifically for Bean Validation failures
 * ({@code MethodArgumentNotValidException}, {@code ConstraintViolationException}).
 *
 * <p>The {@code fieldErrors} map provides per-field messages so the frontend
 * can render inline validation feedback without additional parsing.</p>
 *
 * <p>Example JSON output:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2026-06-21T10:30:00",
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "Request validation failed",
 *   "path": "/api/citizens",
 *   "fieldErrors": {
 *     "email": "must be a valid email address",
 *     "name":  "must not be blank",
 *     "nic":   "must not be blank"
 *   }
 * }
 * }</pre>
 */
@Schema(description = "Validation error response returned when request body fields fail Bean Validation. "
                    + "The fieldErrors map contains per-field error messages.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationErrorResponse {

    @Schema(description = "Timestamp of the error.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code.", example = "400")
    private int status;

    @Schema(description = "Short error phrase.", example = "Validation Failed")
    private String error;

    @Schema(description = "General validation failure message.", example = "Request validation failed")
    private String message;

    @Schema(description = "The request URI.", example = "/api/v1/citizens")
    private String path;

    @Schema(description = "Map of field name to validation error message.",
            example = "{\"email\": \"must be a valid email address\", \"name\": \"must not be blank\"}")
    private Map<String, String> fieldErrors;
}
