package com.govtech.platform.common.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response body returned by the {@link com.govtech.platform.common.exception.GlobalExceptionHandler}
 * for all non-validation error scenarios (4xx, 5xx).
 *
 * <p>The {@code path} field reflects the request URI so clients and log aggregators
 * can correlate errors without inspecting HTTP headers.</p>
 *
 * <p>The {@code message} field contains a human-readable description safe for client
 * consumption. Internal stack traces or technical details are never included.</p>
 *
 * <p>Example JSON output:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2026-06-21T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Citizen not found with citizenReference: CIT-8F3A91B2",
 *   "path": "/api/citizens/CIT-8F3A91B2"
 * }
 * }</pre>
 */
@Schema(description = "Standard error response returned for 4xx and 5xx errors.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    @Schema(description = "Timestamp of the error.", example = "2026-06-21T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code.", example = "404")
    private int status;

    @Schema(description = "Short HTTP reason phrase.", example = "Not Found")
    private String error;

    @Schema(description = "Human-readable error description safe for API clients. Never includes stack traces.",
            example = "Citizen not found: CIT-8F3A91B2")
    private String message;

    @Schema(description = "The request URI that triggered the error.", example = "/api/v1/citizens/CIT-8F3A91B2")
    private String path;
}
