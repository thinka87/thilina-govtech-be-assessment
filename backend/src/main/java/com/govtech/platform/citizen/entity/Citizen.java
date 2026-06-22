package com.govtech.platform.citizen.entity;

import com.govtech.platform.auth.entity.User;
import com.govtech.platform.common.enums.CitizenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Holds citizen-specific profile and business data.
 *
 * <p>Every citizen is linked to exactly one {@link User} account for authentication.
 * The relationship is unidirectional (Citizen → User) to keep the auth module
 * decoupled from the citizen domain and to avoid JSON serialisation cycles.</p>
 *
 * <p>The {@code citizenReference} is a human-readable unique identifier (e.g.
 * {@code CIT-20240001}) used in API paths and cross-module references instead
 * of exposing internal auto-increment IDs.</p>
 */
@Entity
@Table(name = "citizens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** System-generated, human-readable unique identifier. e.g. {@code CIT-20240001}. */
    @Column(nullable = false, unique = true, length = 100)
    private String citizenReference;

    /**
     * Linked user account used for authentication and authorisation.
     * Unique constraint enforces the 1-to-1 relationship at the DB level.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 200)
    private String name;

    /** National Identity Card number — unique when provided; nullable for citizens registered without one. */
    @Column(nullable = true, unique = true, length = 12)
    private String nic;

    /**
     * Citizen email address. Also used as the {@code username} in the linked
     * {@link User} account to allow password-based login.
     */
    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, unique = true, length = 10)
    private String mobileNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CitizenStatus status = CitizenStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = CitizenStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
