package com.govtech.platform.servicerequest.repository;

import com.govtech.platform.servicerequest.entity.ServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link ServiceRequest} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@code ServiceRequestSpecification} predicates (citizen, status, service type).
 * The {@link #findByCitizen_CitizenReference} method powers the citizen self-service
 * endpoint where citizens list only their own requests.</p>
 */
@Repository
public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, Long>, JpaSpecificationExecutor<ServiceRequest> {

    /**
     * Finds a service request by its system-generated reference.
     * Used as the primary business key in API paths.
     */
    Optional<ServiceRequest> findByRequestReference(String requestReference);

    /**
     * Returns a paginated list of service requests belonging to a specific citizen.
     * Used in the citizen self-service endpoint to enforce row-level access control.
     *
     * @param citizenReference the unique citizen reference (e.g. {@code CIT-8F3A91B2})
     * @param pageable         pagination and sorting configuration
     * @return a page of the citizen's service requests
     */
    Page<ServiceRequest> findByCitizen_CitizenReference(String citizenReference, Pageable pageable);

    /** Returns {@code true} if a request with the given reference already exists. */
    boolean existsByRequestReference(String requestReference);
}
