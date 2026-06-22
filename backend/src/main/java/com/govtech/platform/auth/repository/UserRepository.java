package com.govtech.platform.auth.repository;

import com.govtech.platform.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Data access layer for {@link User} entities.
 *
 * <p>Spring Security's {@code UserDetailsService} uses {@link #findByUsername}
 * during authentication. The {@link #existsByUsername} check is used during
 * registration/citizen-creation to enforce uniqueness before persisting.</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their unique login username.
     * Used by Spring Security to load user details during authentication.
     *
     * @param username the login username (typically the citizen's email)
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether a username is already taken.
     * Used during citizen creation to prevent duplicate accounts.
     *
     * @param username the username to check
     * @return {@code true} if a user with this username already exists
     */
    boolean existsByUsername(String username);
}
