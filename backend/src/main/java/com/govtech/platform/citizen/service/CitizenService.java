package com.govtech.platform.citizen.service;

import com.govtech.platform.auth.entity.User;
import com.govtech.platform.auth.repository.UserRepository;
import com.govtech.platform.citizen.dto.*;
import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.CitizenStatus;
import com.govtech.platform.common.enums.Role;
import com.govtech.platform.common.exception.BusinessException;
import com.govtech.platform.common.exception.DuplicateResourceException;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import com.govtech.platform.common.response.PageResponse;
import com.govtech.platform.common.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Business logic for citizen profile management.
 *
 * <h2>Citizen-User linking</h2>
 * <p>Every citizen profile is linked to exactly one {@link User} account. The email
 * address is used as the User's username. Both are created atomically in a single
 * transaction via {@link #createCitizen}.</p>
 *
 * <h2>Temporary password</h2>
 * <p>On creation, a BCrypt-hashed temporary password is stored against the User.
 * {@code mustChangePassword} is set to {@code true}. The plaintext password is
 * never stored or returned after this operation. In production, it must be delivered
 * to the citizen via a secure out-of-band channel.</p>
 *
 * <h2>Deactivation (soft delete)</h2>
 * <p>Citizens are never physically deleted. Deactivation sets
 * {@code Citizen.status = INACTIVE} and {@code User.active = false}, preventing login.
 * All linked service requests and audit history are preserved.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CitizenService {

    private static final String TEMP_PASSWORD_NOTE =
            "Temporary password was set during citizen creation. "
          + "In production, this should be delivered through a secure channel "
          + "(email/SMS/identity verification) and changed after first login.";

    private final CitizenRepository citizenRepository;
    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;

    // ── Create ───────────────────────────────────────────────────────────────

    /**
     * Creates a new citizen profile and a linked User login account atomically.
     *
     * @param request the citizen creation payload
     * @return the created citizen response (includes mustChangePassword and note)
     * @throws DuplicateResourceException if the NIC or email is already registered
     */
    @Transactional
    public CitizenCreatedResponse createCitizen(CreateCitizenRequest request) {
        // ── Normalize NIC to uppercase (871840504v → 871840504V) ──────────
        String nicValue = StringUtils.hasText(request.getNic())
                ? request.getNic().trim().toUpperCase() : null;

        // ── Uniqueness guards ──────────────────────────────────────────────
        if (nicValue != null && citizenRepository.existsByNic(nicValue)) {
            throw new DuplicateResourceException("Citizen", "NIC", nicValue);
        }
        // Username = email; checks both citizen email and any other user with same username
        if (userRepository.existsByUsername(request.getEmail())) {
            throw new DuplicateResourceException("User account", "email", request.getEmail());
        }
        if (citizenRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new DuplicateResourceException("Citizen", "mobile number", request.getMobileNumber());
        }

        // ── Create linked User account ─────────────────────────────────────
        User user = User.builder()
                .username(request.getEmail())
                .password(passwordEncoder.encode(request.getTemporaryPassword()))
                .role(Role.CITIZEN)
                .active(true)
                .mustChangePassword(true)   // must change on first login
                .build();
        userRepository.save(user);
        Citizen citizen = Citizen.builder()
                .citizenReference(ReferenceGenerator.generateCitizenReference())
                .user(user)
                .name(request.getName())
                .nic(nicValue)
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .address(request.getAddress())
                .status(CitizenStatus.ACTIVE)
                .build();
        citizenRepository.save(citizen);

        log.info("Created citizen '{}' (ref: {}) linked to user '{}'",
                citizen.getName(), citizen.getCitizenReference(), user.getUsername());

        return mapToCreatedResponse(citizen);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns the full citizen profile for a given reference.
     *
     * @param citizenReference the unique citizen reference
     * @return the full citizen response DTO
     * @throws ResourceNotFoundException if no citizen exists with this reference
     */
    @Transactional(readOnly = true)
    public CitizenResponse getCitizenByReference(String citizenReference) {
        Citizen citizen = findCitizenOrThrow(citizenReference);
        return mapToCitizenResponse(citizen);
    }

    /**
     * Returns a paginated list of citizen summaries, optionally filtered by a keyword.
     *
     * <p>When {@code search} is blank, all citizens are returned. When provided,
     * the keyword is matched case-insensitively against name, NIC, email, and mobile.</p>
     *
     * @param search   optional search keyword
     * @param pageable pagination and sorting configuration
     * @return a paged list of citizen summary DTOs
     */
    @Transactional(readOnly = true)
    public PageResponse<CitizenSummaryResponse> listCitizens(String search, CitizenStatus status, Pageable pageable) {
        Page<Citizen> page;
        boolean hasSearch = StringUtils.hasText(search);

        if (hasSearch && status != null) {
            page = citizenRepository.searchCitizensByStatus(search.trim(), status, pageable);
        } else if (hasSearch) {
            page = citizenRepository.searchCitizens(search.trim(), pageable);
        } else if (status != null) {
            page = citizenRepository.findByStatus(status, pageable);
        } else {
            page = citizenRepository.findAll(pageable);
        }

        return PageResponse.from(page.map(this::mapToSummaryResponse));
    }

    // ── Update ───────────────────────────────────────────────────────────────

    /**
     * Updates a citizen's profile information.
     *
     * <p>If the email changes, the linked User's username is also updated.
     * The citizen's existing JWT will become invalid (username mismatch) on the
     * next request — they must re-login with the new email.</p>
     *
     * <p>NIC cannot be changed — it is a permanent government identifier.</p>
     *
     * @param citizenReference the citizen to update
     * @param request          the update payload
     * @return the updated citizen response DTO
     */
    @Transactional
    public CitizenResponse updateCitizen(String citizenReference, UpdateCitizenRequest request) {
        Citizen citizen = findCitizenOrThrow(citizenReference);

        // ── Email / username change handling ───────────────────────────────
        String newEmail = request.getEmail().trim();
        if (!citizen.getEmail().equalsIgnoreCase(newEmail)) {
            if (userRepository.existsByUsername(newEmail)) {
                throw new DuplicateResourceException("User account", "email", newEmail);
            }
            // Update the linked User's username to match the new email
            User user = citizen.getUser();
            user.setUsername(newEmail);
            userRepository.save(user);
            citizen.setEmail(newEmail);

            log.info("Updated email/username for citizen '{}': {} → {}",
                    citizenReference, citizen.getEmail(), newEmail);
        }

        // ── Mobile uniqueness check ────────────────────────────────────────
        String newMobile = request.getMobileNumber().trim();
        if (!citizen.getMobileNumber().equals(newMobile)
                && citizenRepository.existsByMobileNumberAndCitizenReferenceNot(newMobile, citizenReference)) {
            throw new DuplicateResourceException("Citizen", "mobile number", newMobile);
        }

        // ── Update profile fields ──────────────────────────────────────────
        citizen.setName(request.getName());
        citizen.setMobileNumber(newMobile);
        citizen.setAddress(request.getAddress());

        // ── Optional status update ─────────────────────────────────────────
        if (request.getStatus() != null && request.getStatus() != citizen.getStatus()) {
            applyStatusChange(citizen, request.getStatus());
        }

        citizenRepository.save(citizen);

        log.info("Updated citizen profile '{}'", citizenReference);
        return mapToCitizenResponse(citizen);
    }

    // ── Deactivate ────────────────────────────────────────────────────────────

    /**
     * Soft-deactivates a citizen profile.
     *
     * <p>Sets {@code Citizen.status = INACTIVE} and {@code User.active = false}.
     * The citizen can no longer log in. All linked service requests and status
     * history records are preserved for audit purposes.</p>
     *
     * @param citizenReference the citizen to deactivate
     * @throws BusinessException         if the citizen is already inactive
     * @throws ResourceNotFoundException if no citizen exists with this reference
     */
    @Transactional
    public void deactivateCitizen(String citizenReference) {
        Citizen citizen = findCitizenOrThrow(citizenReference);

        if (citizen.getStatus() == CitizenStatus.INACTIVE) {
            throw new BusinessException(
                    "Citizen '" + citizenReference + "' is already inactive.");
        }

        citizen.setStatus(CitizenStatus.INACTIVE);
        citizenRepository.save(citizen);

        User user = citizen.getUser();
        user.setActive(false);
        userRepository.save(user);

        log.info("Deactivated citizen '{}' and their User account '{}'",
                citizenReference, user.getUsername());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Citizen findCitizenOrThrow(String citizenReference) {
        return citizenRepository.findByCitizenReference(citizenReference)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Citizen", "citizenReference", citizenReference));
    }

    /**
     * Applies a status change to the citizen and syncs the linked User's active flag.
     * Must be called inside a transaction.
     */
    private void applyStatusChange(Citizen citizen, CitizenStatus newStatus) {
        citizen.setStatus(newStatus);
        User user = citizen.getUser();
        user.setActive(newStatus == CitizenStatus.ACTIVE);
        userRepository.save(user);
    }

    // ── Mapping methods ───────────────────────────────────────────────────────

    /**
     * Maps a {@link Citizen} entity to the full {@link CitizenResponse} DTO.
     * Triggers a lazy load of {@code citizen.user} — safe inside a transaction.
     */
    private CitizenResponse mapToCitizenResponse(Citizen citizen) {
        return CitizenResponse.builder()
                .citizenReference(citizen.getCitizenReference())
                .name(citizen.getName())
                .nic(citizen.getNic())
                .email(citizen.getEmail())
                .mobileNumber(citizen.getMobileNumber())
                .address(citizen.getAddress())
                .status(citizen.getStatus())
                .username(citizen.getUser().getUsername())
                .mustChangePassword(citizen.getUser().isMustChangePassword())
                .createdAt(citizen.getCreatedAt())
                .updatedAt(citizen.getUpdatedAt())
                .build();
    }

    /**
     * Maps a {@link Citizen} entity to the lightweight {@link CitizenSummaryResponse} DTO.
     * Does NOT access {@code citizen.user} — avoids N+1 queries in paginated lists.
     */
    private CitizenSummaryResponse mapToSummaryResponse(Citizen citizen) {
        return CitizenSummaryResponse.builder()
                .citizenReference(citizen.getCitizenReference())
                .name(citizen.getName())
                .nic(citizen.getNic())
                .email(citizen.getEmail())
                .mobileNumber(citizen.getMobileNumber())
                .status(citizen.getStatus())
                .build();
    }

    /**
     * Maps a newly created {@link Citizen} entity to {@link CitizenCreatedResponse}.
     * Includes {@code mustChangePassword} and the informational {@code temporaryPasswordNote}.
     * The encoded password is never included.
     */
    private CitizenCreatedResponse mapToCreatedResponse(Citizen citizen) {
        return CitizenCreatedResponse.builder()
                .citizenReference(citizen.getCitizenReference())
                .name(citizen.getName())
                .nic(citizen.getNic())
                .email(citizen.getEmail())
                .mobileNumber(citizen.getMobileNumber())
                .address(citizen.getAddress())
                .status(citizen.getStatus())
                .username(citizen.getUser().getUsername())
                .mustChangePassword(citizen.getUser().isMustChangePassword())
                .temporaryPasswordNote(TEMP_PASSWORD_NOTE)
                .createdAt(citizen.getCreatedAt())
                .build();
    }
}
