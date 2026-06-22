package com.govtech.platform.common.exception;

/**
 * Thrown when an invalid service request status transition is attempted.
 *
 * <p>Maps to HTTP 400 Bad Request via the {@link GlobalExceptionHandler}.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Trying to move a CANCELLED request back to SUBMITTED</li>
 *   <li>Trying to APPROVE a request that is already REJECTED</li>
 *   <li>Providing an unrecognised status value in a request payload</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Single message
 * throw new InvalidStatusException("Cannot transition a CANCELLED request");
 *
 * // Structured transition message
 * throw new InvalidStatusException("CANCELLED", "SUBMITTED");
 * // → "Invalid status transition from CANCELLED to SUBMITTED"
 * }</pre>
 */
public class InvalidStatusException extends RuntimeException {

    public InvalidStatusException(String message) {
        super(message);
    }

    /**
     * Convenience constructor for explicit transition failures.
     *
     * @param fromStatus the current status as a string
     * @param toStatus   the requested (invalid) target status
     */
    public InvalidStatusException(String fromStatus, String toStatus) {
        super("Invalid status transition from " + fromStatus + " to " + toStatus);
    }
}
