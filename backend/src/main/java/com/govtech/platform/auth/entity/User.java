package com.govtech.platform.auth.entity;

import com.govtech.platform.common.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Stores authentication and authorisation data for every user in the system,
 * regardless of role (CITIZEN, SERVICE_AGENT, ADMIN).
 *
 * <p>Passwords are always stored as BCrypt hashes — never in plaintext.
 * The {@code toString()} method explicitly excludes the password field to
 * prevent accidental exposure in logs.</p>
 *
 * <p>For citizen accounts created by an admin, {@code mustChangePassword} is
 * set to {@code true} and the temporary password must be communicated securely
 * (e.g. via email or SMS) outside this system. The temporary password is
 * returned only once in the admin create-citizen response and is never stored
 * in plaintext after hashing.</p>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique login identifier. For citizen accounts this is typically the citizen's email. */
    @Column(nullable = false, unique = true, length = 150)
    private String username;

    /**
     * BCrypt-hashed password. Never exposed in API responses.
     * See {@link #toString()} for the explicit exclusion.
     */
    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Whether the account is allowed to authenticate. Defaults to {@code true}. */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /**
     * Signals that the user must update their password on next login.
     * Always {@code true} for newly created citizen accounts (temporary password flow).
     * {@code false} for admin- and service-agent accounts created directly.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    /**
     * Incremented on every successful login. The current value is embedded as
     * a {@code ver} claim in the issued JWT. The JWT filter rejects any token
     * whose {@code ver} claim does not match this field, invalidating all
     * previously issued tokens for this user.
     */
    @Builder.Default
    @Column(nullable = false)
    private int tokenVersion = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Password is intentionally excluded to prevent accidental log exposure. */
    @Override
    public String toString() {
        return "User{id=" + id
                + ", username='" + username + '\''
                + ", role=" + role
                + ", active=" + active
                + ", mustChangePassword=" + mustChangePassword
                + '}';
    }
}
