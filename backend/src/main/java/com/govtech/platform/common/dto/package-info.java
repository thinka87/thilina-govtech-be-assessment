/**
 * DTO (Data Transfer Object) conventions for this platform.
 *
 * <h2>Principles</h2>
 * <ul>
 *   <li><strong>JPA entities are NEVER returned directly from controllers.</strong>
 *       Entity classes contain persistence metadata, lazy-loaded associations, and
 *       sensitive fields (e.g. {@code User.password}) that must not leak into
 *       API responses.</li>
 *
 *   <li><strong>Request DTOs</strong> carry incoming data from the client.
 *       They use Bean Validation annotations ({@code @NotBlank}, {@code @Email}, etc.)
 *       to enforce input contracts at the controller boundary.</li>
 *
 *   <li><strong>Response DTOs</strong> carry outgoing data to the client.
 *       They are constructed in the service layer by mapping from entities.
 *       They expose only the fields the API contract requires, keeping the
 *       response stable even if the internal entity model changes.</li>
 * </ul>
 *
 * <h2>DTO placement</h2>
 * <p>Each module owns its own {@code dto} sub-package:</p>
 * <ul>
 *   <li>{@code com.govtech.platform.auth.dto}           – login/register payloads, JWT response</li>
 *   <li>{@code com.govtech.platform.citizen.dto}        – citizen request/response DTOs</li>
 *   <li>{@code com.govtech.platform.servicerequest.dto} – service request request/response DTOs</li>
 *   <li>{@code com.govtech.platform.document.dto}       – document metadata DTOs</li>
 *   <li>{@code com.govtech.platform.notification.dto}   – notification response DTOs</li>
 *   <li>{@code com.govtech.platform.statushistory.dto}  – status history response DTOs</li>
 * </ul>
 *
 * <h2>Password security</h2>
 * <p>The {@code User.password} field is:</p>
 * <ul>
 *   <li>Excluded from all response DTOs</li>
 *   <li>Excluded from {@code User.toString()}</li>
 *   <li>Never logged by application code</li>
 *   <li>Always stored as a BCrypt hash — never in plaintext</li>
 * </ul>
 * <p>In production, temporary citizen passwords must be delivered via secure
 * out-of-band channels (email/SMS) — not stored or returned after the creation response.</p>
 */
package com.govtech.platform.common.dto;
