package com.govtech.platform.common.enums;

/**
 * Verification state of a supporting document attached to a service request.
 *
 * <ul>
 *   <li>PENDING  – uploaded, awaiting agent review.</li>
 *   <li>VERIFIED – agent confirmed document is valid.</li>
 *   <li>REJECTED – agent marked document as invalid or unacceptable.</li>
 * </ul>
 */
public enum DocumentVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
