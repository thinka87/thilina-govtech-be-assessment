package com.govtech.platform.auth.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Extends Spring Security's {@link UserDetails} to carry the user's
 * {@code tokenVersion} alongside the standard authentication fields.
 *
 * <p>This allows the {@link JwtAuthenticationFilter} to compare the version
 * embedded in the incoming JWT against the current DB value without a
 * second database round-trip.</p>
 */
public class CustomUserDetails extends org.springframework.security.core.userdetails.User {

    private final int tokenVersion;

    public CustomUserDetails(UserDetails delegate, int tokenVersion) {
        super(
            delegate.getUsername(),
            delegate.getPassword(),
            delegate.isEnabled(),
            delegate.isAccountNonExpired(),
            delegate.isCredentialsNonExpired(),
            delegate.isAccountNonLocked(),
            delegate.getAuthorities()
        );
        this.tokenVersion = tokenVersion;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }
}
