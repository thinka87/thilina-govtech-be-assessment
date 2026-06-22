package com.govtech.platform.citizen.repository;

import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.common.enums.CitizenStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link Citizen} entities.
 *
 * <p>The {@link #searchCitizens} JPQL query performs a case-insensitive partial-match
 * search across name, NIC, email, and mobile number using a single keyword. The single
 * parameter is bound to all four conditions to allow the service to pass the same
 * search term once.</p>
 *
 * <p>When no search term is provided, the service calls {@link #findAll(Pageable)}
 * directly — no need for a redundant repository method.</p>
 */
@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {

    /**
     * Finds a citizen by their unique system reference.
     * Primary lookup used in API path variables (e.g. {@code /citizens/{citizenReference}}).
     */
    Optional<Citizen> findByCitizenReference(String citizenReference);

    /** Finds a citizen by NIC. Used for uniqueness checks and agent lookups. */
    Optional<Citizen> findByNic(String nic);

    /**
     * Finds a citizen by the username of their linked User account.
     * Used in citizen-scoped endpoints to resolve the authenticated citizen from the JWT principal.
     */
    Optional<Citizen> findByUser_Username(String username);

    /** Returns {@code true} if a citizen with the given reference already exists. */
    boolean existsByCitizenReference(String citizenReference);

    /** Returns {@code true} if a citizen with the given NIC already exists. */
    boolean existsByNic(String nic);

    /** Returns {@code true} if a citizen with the given mobile number already exists. */
    boolean existsByMobileNumber(String mobileNumber);

    /**
     * Returns {@code true} if another citizen (different citizenReference) has the same mobile number.
     * Used for uniqueness checks during profile updates.
     */
    boolean existsByMobileNumberAndCitizenReferenceNot(String mobileNumber, String citizenReference);

    /**
     * Returns {@code true} if a citizen with the given email already exists.
     * Used as a secondary uniqueness check before creating a linked User account.
     */
    boolean existsByEmail(String email);

    /**
     * Case-insensitive partial-match search across citizen name, NIC, email, and mobile number.
     *
     * <p>The same {@code keyword} is compared against all four fields using LIKE.
     * Results are paged according to the provided {@link Pageable}.</p>
     *
     * <p>Example: searching "kamal" matches citizens where name contains "kamal",
     * or NIC contains "kamal", or email contains "kamal", or mobile contains "kamal".</p>
     *
     * @param keyword  the search keyword (single term, partial match)
     * @param pageable pagination and sorting configuration
     * @return a page of matching citizen records
     */
    @Query("""
            SELECT c FROM Citizen c
            WHERE LOWER(c.name)         LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.nic)          LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.email)        LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.mobileNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Citizen> searchCitizens(@Param("keyword") String keyword, Pageable pageable);

    /** Filters all citizens by status. */
    Page<Citizen> findByStatus(CitizenStatus status, Pageable pageable);

    /** Case-insensitive keyword search filtered by status. */
    @Query("""
            SELECT c FROM Citizen c
            WHERE c.status = :status
              AND (LOWER(c.name)         LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR  LOWER(c.nic)          LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR  LOWER(c.email)        LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR  LOWER(c.mobileNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Citizen> searchCitizensByStatus(
            @Param("keyword") String keyword,
            @Param("status") CitizenStatus status,
            Pageable pageable);
}
