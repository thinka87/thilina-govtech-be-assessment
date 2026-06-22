package com.govtech.platform.common.exception;

/**
 * Thrown when a requested resource does not exist in the system.
 *
 * <p>Maps to HTTP 404 Not Found via the {@link GlobalExceptionHandler}.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Citizen not found by citizenReference or NIC</li>
 *   <li>Service request not found by requestReference</li>
 *   <li>Supporting document not found by documentReference</li>
 *   <li>Notification not found by ID</li>
 * </ul>
 *
 * <p>Usage examples:</p>
 * <pre>{@code
 * // Single message
 * throw new ResourceNotFoundException("Citizen not found");
 *
 * // Structured message (resource type + field + value)
 * throw new ResourceNotFoundException("Citizen", "citizenReference", citizenRef);
 * // → "Citizen not found with citizenReference: CIT-8F3A91B2"
 * }</pre>
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor that produces a consistent message format.
     *
     * @param resourceType the domain type, e.g. "Citizen", "ServiceRequest"
     * @param field        the lookup field, e.g. "citizenReference", "NIC"
     * @param value        the value that was searched for
     */
    public ResourceNotFoundException(String resourceType, String field, String value) {
        super(resourceType + " not found with " + field + ": " + value);
    }
}
