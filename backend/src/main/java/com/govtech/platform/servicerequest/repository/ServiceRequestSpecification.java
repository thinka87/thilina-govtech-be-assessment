package com.govtech.platform.servicerequest.repository;

import com.govtech.platform.common.enums.ServiceRequestStatus;
import com.govtech.platform.servicerequest.entity.ServiceRequest;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA {@link Specification} factories for dynamic {@link ServiceRequest} filtering.
 *
 * <p>Each static method returns a composable predicate. Combine them with
 * {@link Specification#and} to build a compound query:</p>
 *
 * <pre>
 * Specification&lt;ServiceRequest&gt; spec = Specification.where(null);
 * if (citizenRef != null) spec = spec.and(ServiceRequestSpecification.byCitizenReference(citizenRef));
 * if (status     != null) spec = spec.and(ServiceRequestSpecification.byStatus(status));
 * if (serviceType != null) spec = spec.and(ServiceRequestSpecification.byServiceTypeContaining(serviceType));
 * repository.findAll(spec, pageable);
 * </pre>
 */
public class ServiceRequestSpecification {

    private ServiceRequestSpecification() {
        // utility class — no instances
    }

    /**
     * Filters service requests belonging to the given citizen reference.
     *
     * @param citizenReference the target citizen reference (e.g. {@code CIT-8F3A91B2})
     */
    public static Specification<ServiceRequest> byCitizenReference(String citizenReference) {
        return (root, query, cb) ->
                cb.equal(root.get("citizen").get("citizenReference"), citizenReference);
    }

    /**
     * Filters service requests with the exact given status.
     *
     * @param status the {@link ServiceRequestStatus} to filter by
     */
    public static Specification<ServiceRequest> byStatus(ServiceRequestStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }

    /**
     * Filters service requests whose {@code serviceType} contains the given keyword
     * (case-insensitive partial match).
     *
     * @param keyword the partial service type string to match
     */
    public static Specification<ServiceRequest> byServiceTypeContaining(String keyword) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("serviceType")),
                        "%" + keyword.toLowerCase() + "%");
    }
}
