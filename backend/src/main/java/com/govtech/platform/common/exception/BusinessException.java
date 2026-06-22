package com.govtech.platform.common.exception;

/**
 * Thrown for general business rule violations that do not fit a more
 * specific exception category.
 *
 * <p>Maps to HTTP 400 Bad Request via the {@link GlobalExceptionHandler}.</p>
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Submitting a service request for an INACTIVE citizen</li>
 *   <li>Attempting to upload a document to a CLOSED request</li>
 *   <li>Any domain invariant that is violated at the service layer</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * throw new BusinessException("Cannot submit a service request for an INACTIVE citizen");
 * }</pre>
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
