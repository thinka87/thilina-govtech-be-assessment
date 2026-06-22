package com.govtech.platform.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Generic success response wrapper used for create / update / delete operations
 * and other endpoints where a simple {@code data} + {@code message} envelope adds clarity.
 *
 * <p>Not every endpoint needs this wrapper — simple GET list or detail endpoints may
 * return the DTO directly. Use {@link ApiResponse} when a success flag and message
 * meaningfully improve the client contract (e.g. citizen creation, status update).</p>
 *
 * <p>Example JSON output:</p>
 * <pre>{@code
 * {
 *   "success": true,
 *   "message": "Citizen created successfully",
 *   "data": { ... }
 * }
 * }</pre>
 *
 * @param <T> the type of the response payload
 */
@Schema(description = "Generic success response wrapper for create, update, and delete operations.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    @Schema(description = "Always true for success responses.", example = "true")
    private boolean success;

    @Schema(description = "Human-readable success message.", example = "Operation completed successfully.")
    private String message;

    @Schema(description = "The response payload. Null for delete/deactivate operations.")
    private T data;

    // ── Factory helpers ───────────────────────────────────────────────────────

    /** Creates a success response with a custom message and payload. */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /** Creates a success response with a default message and payload. */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("Operation completed successfully")
                .data(data)
                .build();
    }

    /** Creates a success response with only a message and no data payload (e.g. delete). */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .data(null)
                .build();
    }
}
