package com.govtech.platform.common.enums;

/**
 * Lifecycle states of a service request.
 *
 * <pre>
 *   SUBMITTED → IN_REVIEW → APPROVED
 *                         → REJECTED
 *   Any state → CANCELLED (by citizen or admin)
 * </pre>
 */
public enum ServiceRequestStatus {
    SUBMITTED,
    IN_REVIEW,
    APPROVED,
    REJECTED,
    CANCELLED
}
