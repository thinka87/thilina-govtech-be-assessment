package com.govtech.platform.common.enums;

/**
 * Lifecycle status of a citizen profile.
 *
 * <ul>
 *   <li>ACTIVE   – citizen can log in and submit service requests.</li>
 *   <li>INACTIVE – citizen account is suspended; login is denied.</li>
 * </ul>
 */
public enum CitizenStatus {
    ACTIVE,
    INACTIVE
}
