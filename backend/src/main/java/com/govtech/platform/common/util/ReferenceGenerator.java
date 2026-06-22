package com.govtech.platform.common.util;

import java.util.UUID;

/**
 * Generates human-readable, collision-resistant unique references for domain entities.
 *
 * <p>Format: {@code PREFIX-XXXXXXXX} where {@code XXXXXXXX} is 8 uppercase hex characters
 * derived from a random UUID. This gives 16^8 = ~4.3 billion unique values per prefix.</p>
 *
 * <p><strong>Collision probability:</strong> negligible at assessment or low-volume
 * production scale. For high-throughput production systems, replace with a DB-sequence-backed
 * generator (e.g. {@code CIT-2026-000001}) or use a full UUID.</p>
 *
 * <p>Examples:</p>
 * <pre>
 *   CIT-8F3A91B2
 *   REQ-2B71AC3D
 *   DOC-9A21BE4F
 * </pre>
 *
 * <p>This is a utility class — it cannot be instantiated.</p>
 */
public final class ReferenceGenerator {

    private ReferenceGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ── Public factory methods ────────────────────────────────────────────────

    /**
     * Generates a unique citizen reference.
     *
     * @return e.g. {@code CIT-8F3A91B2}
     */
    public static String generateCitizenReference() {
        return "CIT-" + shortUuid();
    }

    /**
     * Generates a unique service request reference.
     *
     * @return e.g. {@code REQ-2B71AC3D}
     */
    public static String generateServiceRequestReference() {
        return "REQ-" + shortUuid();
    }

    /**
     * Generates a unique supporting document reference.
     *
     * @return e.g. {@code DOC-9A21BE4F}
     */
    public static String generateDocumentReference() {
        return "DOC-" + shortUuid();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns the first 8 hex characters of a random UUID in uppercase.
     *
     * <p>A UUID has 122 bits of randomness. Taking 8 hex chars (32 bits) is more than
     * sufficient for assessment-scale uniqueness. The remaining UUID entropy is discarded.</p>
     */
    private static String shortUuid() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
}
