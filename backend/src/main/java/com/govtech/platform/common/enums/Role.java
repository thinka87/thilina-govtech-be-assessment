package com.govtech.platform.common.enums;

/**
 * Roles assigned to user accounts. Controls API access and UI rendering.
 *
 * <ul>
 *   <li>CITIZEN       – registered citizen; submits and tracks service requests.</li>
 *   <li>SERVICE_AGENT – government staff; reviews and processes requests.</li>
 *   <li>ADMIN         – platform administrator; manages users and configuration.</li>
 * </ul>
 */
public enum Role {
    CITIZEN,
    SERVICE_AGENT,
    ADMIN
}
