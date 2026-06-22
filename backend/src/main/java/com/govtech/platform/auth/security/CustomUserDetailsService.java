package com.govtech.platform.auth.security;

import com.govtech.platform.auth.entity.User;
import com.govtech.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation.
 *
 * <p>Loads a {@link User} entity from the database and converts it to
 * a Spring Security {@link UserDetails} object used during authentication
 * and JWT filter processing.</p>
 *
 * <p>Role mapping: the platform {@link com.govtech.platform.common.enums.Role}
 * enum value is prefixed with {@code ROLE_} to comply with Spring Security's
 * authority naming convention:</p>
 * <ul>
 *   <li>ADMIN         → ROLE_ADMIN</li>
 *   <li>SERVICE_AGENT → ROLE_SERVICE_AGENT</li>
 *   <li>CITIZEN       → ROLE_CITIZEN</li>
 * </ul>
 *
 * <p>The {@code active} flag is mapped to {@code enabled} and {@code accountNonLocked},
 * so inactive users will be rejected by Spring Security during authentication.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No user found with username: " + username));

        UserDetails base = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isActive())
                .accountLocked(!user.isActive())
                .build();
        return new CustomUserDetails(base, user.getTokenVersion());
    }
}
