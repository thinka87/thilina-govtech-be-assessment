package com.govtech.platform.common.exception;

/**
 * Thrown when an authenticated user attempts to perform an action on a resource
 * they are not permitted to access, regardless of their role.
 *
 * <p>Maps to HTTP 403 Forbidden via the {@link GlobalExceptionHandler}.</p>
 *
 * <p>This is distinct from Spring Security's {@code AccessDeniedException}:
 * it is thrown at the <em>service layer</em> for fine-grained business-level
 * access control (e.g. row-level ownership checks) rather than at the
 * security filter level.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>A CITIZEN trying to read another citizen's service requests</li>
 *   <li>A SERVICE_AGENT trying to modify admin-only resources</li>
 *   <li>Any ownership violation not caught by Spring Security method security</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * throw new UnauthorizedActionException(
 *     "You are not authorised to access this service request");
 * }</pre>
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
