package com.govtech.platform.common.exception;

/**
 * Thrown when attempting to create a resource that would violate a uniqueness constraint.
 *
 * <p>Maps to HTTP 409 Conflict via the {@link GlobalExceptionHandler}.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Duplicate username or email when creating a user/citizen</li>
 *   <li>Duplicate NIC when creating a citizen profile</li>
 *   <li>Duplicate citizenReference, requestReference, or documentReference</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Single message
 * throw new DuplicateResourceException("A citizen with this NIC already exists");
 *
 * // Structured message
 * throw new DuplicateResourceException("Citizen", "NIC", nic);
 * // → "Citizen already exists with NIC: 199012345678"
 * }</pre>
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    /**
     * Convenience constructor that produces a consistent message format.
     *
     * @param resourceType the domain type, e.g. "Citizen", "User"
     * @param field        the unique field, e.g. "NIC", "username"
     * @param value        the duplicate value
     */
    public DuplicateResourceException(String resourceType, String field, String value) {
        super(resourceType + " already exists with " + field + ": " + value);
    }
}
