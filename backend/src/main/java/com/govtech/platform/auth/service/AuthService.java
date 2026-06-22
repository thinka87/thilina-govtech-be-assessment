package com.govtech.platform.auth.service;

import com.govtech.platform.auth.dto.ChangePasswordRequest;
import com.govtech.platform.auth.dto.LoginRequest;
import com.govtech.platform.auth.dto.LoginResponse;
import com.govtech.platform.auth.entity.User;
import com.govtech.platform.auth.repository.UserRepository;
import com.govtech.platform.auth.security.JwtService;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.Role;
import com.govtech.platform.common.exception.BusinessException;
import com.govtech.platform.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for authentication and password management.
 *
 * <h2>Login flow</h2>
 * <ol>
 *   <li>Delegate credential verification to Spring Security's {@link AuthenticationManager}.
 *       This throws {@code BadCredentialsException} (→ 401) for invalid credentials and
 *       {@code LockedException} / {@code DisabledException} (→ 401) for inactive accounts.</li>
 *   <li>Load the {@link User} entity to access role and flags.</li>
 *   <li>Guard against inactive accounts (belt-and-suspenders check).</li>
 *   <li>Generate a JWT token and return the {@link LoginResponse}.</li>
 * </ol>
 *
 * <h2>Change-password flow</h2>
 * <ol>
 *   <li>Load the user by username from the JWT principal.</li>
 *   <li>Verify the current password using BCrypt comparison.</li>
 *   <li>Reject if the new password is the same as the current one.</li>
 *   <li>Hash and persist the new password; clear the {@code mustChangePassword} flag.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository        userRepository;
    private final JwtService            jwtService;
    private final PasswordEncoder       passwordEncoder;
    private final CitizenRepository     citizenRepository;

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Authenticates the user and returns a JWT token with role and flag metadata.
     *
     * @param request login credentials
     * @return a populated {@link LoginResponse} containing the JWT token
     */
    public LoginResponse login(LoginRequest request) {
        // Spring Security handles credential verification and account status checks.
        // Throws AuthenticationException subclasses on failure (Spring maps these to 401).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", request.getUsername()));

        // Belt-and-suspenders: reject inactive users even if Spring Security passes them
        if (!user.isActive()) {
            throw new BusinessException(
                    "Your account is inactive. Please contact the administrator.");
        }

        // Increment token version to invalidate all previously issued tokens for this user
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), user.getRole().name(), user.getTokenVersion());

        String citizenRef = null;
        if (user.getRole() == Role.CITIZEN) {
            citizenRef = citizenRepository.findByUser_Username(user.getUsername())
                    .map(c -> c.getCitizenReference())
                    .orElse(null);
        }

        log.info("Login successful for user '{}' with role '{}'",
                user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().name())
                .mustChangePassword(user.isMustChangePassword())
                .citizenReference(citizenRef)
                .build();
    }

    // ── Change password ───────────────────────────────────────────────────────

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param username the authenticated user's username (from JWT principal)
     * @param request  the change-password payload
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "username", username));

        // Verify that the provided current password matches the stored BCrypt hash
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect.");
        }

        // Prevent re-use of the current password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(
                    "New password must be different from the current password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);

        log.info("Password changed successfully for user '{}'", username);
    }
}
