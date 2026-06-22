package com.govtech.platform.config;

import com.govtech.platform.auth.entity.User;
import com.govtech.platform.auth.repository.UserRepository;
import com.govtech.platform.citizen.entity.Citizen;
import com.govtech.platform.citizen.repository.CitizenRepository;
import com.govtech.platform.common.enums.CitizenStatus;
import com.govtech.platform.common.enums.Role;
import com.govtech.platform.common.util.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default users into the database on application startup.
 *
 * <p>Each user is only created if their username does not already exist —
 * safe to run on every restart.</p>
 *
 * <h2>Default users</h2>
 * <table border="1">
 *   <tr><th>Username</th><th>Password</th><th>Role</th><th>Active</th><th>mustChangePassword</th></tr>
 *   <tr><td>admin@gov.lk</td><td>Admin@123</td><td>ADMIN</td><td>true</td><td>false</td></tr>
 *   <tr><td>agent@gov.lk</td><td>Agent@123</td><td>SERVICE_AGENT</td><td>true</td><td>false</td></tr>
 *   <tr><td>citizen@gov.lk</td><td>Citizen@123</td><td>CITIZEN</td><td>true</td><td>true</td></tr>
 *   <tr><td>inactive@gov.lk</td><td>Inactive@123</td><td>CITIZEN</td><td>false</td><td>false</td></tr>
 * </table>
 *
 * <h2>Citizen test user note</h2>
 * <p>The {@code citizen@gov.lk} user account is created here for authentication testing
 * but is <strong>not yet linked to a Citizen profile</strong>. The citizen profile and
 * the User-to-Citizen 1-to-1 link will be established when the Citizen Management
 * module is implemented in Step 5.</p>
 *
 * <p>Until the Citizen profile is linked, calling citizen-scoped endpoints (e.g.
 * "get my profile") will return a 404 — this is expected and correct behaviour.</p>
 *
 * <h2>Production note</h2>
 * <p>In production, seed data should be managed via Flyway migration scripts rather
 * than a {@link CommandLineRunner}. The passwords below are for development and
 * assessment testing only — replace them with strong, unique credentials.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository     userRepository;
    private final CitizenRepository  citizenRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("Seeding default users...");

        seedUser("admin@gov.lk",    "Admin@123",    Role.ADMIN,         true,  false);
        seedUser("agent@gov.lk",    "Agent@123",    Role.SERVICE_AGENT, true,  false);
        seedUser("citizen@gov.lk",  "Citizen@123",  Role.CITIZEN,       true,  true);
        seedUser("inactive@gov.lk", "Inactive@123", Role.CITIZEN,       false, false);

        // Link the seed citizen user to a Citizen profile if not yet linked
        seedCitizenProfile("citizen@gov.lk",
                "Test Citizen",
                "0771234567",
                "123 Main Street, Colombo 03");

        log.info("Default user seeding complete.");
    }

    private void seedUser(String username, String rawPassword, Role role,
                          boolean active, boolean mustChangePassword) {
        if (userRepository.existsByUsername(username)) {
            log.debug("User '{}' already exists — skipping.", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .active(active)
                .mustChangePassword(mustChangePassword)
                .build();

        userRepository.save(user);
        log.info("Created default user: '{}' (role: {}, active: {}, mustChangePassword: {})",
                username, role, active, mustChangePassword);
    }

    private void seedCitizenProfile(String username, String name, String mobileNumber,
                                    String address) {
        userRepository.findByUsername(username).ifPresent(user -> {
            // Skip if a citizen profile is already linked to this user
            if (citizenRepository.findByUser_Username(username).isPresent()) {
                log.debug("Citizen profile for '{}' already exists — skipping.", username);
                return;
            }
            // Skip if a citizen already has this email (may have been created via admin UI)
            if (citizenRepository.existsByEmail(username)) {
                log.debug("Citizen with email '{}' already exists — skipping profile seed.", username);
                return;
            }
            // Ensure mobile number is unique; try sequential fallbacks if taken
            String mobile = mobileNumber;
            int suffix = 1;
            while (citizenRepository.existsByMobileNumber(mobile)) {
                mobile = String.format("07%08d", suffix++);
            }
            Citizen citizen = Citizen.builder()
                    .citizenReference(ReferenceGenerator.generateCitizenReference())
                    .user(user)
                    .name(name)
                    .email(username)
                    .mobileNumber(mobile)
                    .address(address)
                    .status(CitizenStatus.ACTIVE)
                    .build();
            citizenRepository.save(citizen);
            log.info("Created citizen profile for '{}' (ref: {})",
                    username, citizen.getCitizenReference());
        });
    }
}
