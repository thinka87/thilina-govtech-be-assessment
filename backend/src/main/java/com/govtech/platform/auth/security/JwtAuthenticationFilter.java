package com.govtech.platform.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Intercepts every HTTP request exactly once and populates the Spring Security
 * {@link SecurityContextHolder} when a valid JWT Bearer token is present.
 *
 * <p>Processing flow:</p>
 * <ol>
 *   <li>Extract the token from the {@code Authorization: Bearer <token>} header.</li>
 *   <li>If no token is present, skip processing and continue the filter chain
 *       (the downstream {@code ExceptionTranslationFilter} will return 401).</li>
 *   <li>Extract the username from the token.</li>
 *   <li>Load the user from the database via {@link CustomUserDetailsService}.</li>
 *   <li>Validate the token (signature, expiry, subject match).</li>
 *   <li>Set the authentication in the {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p>Any JWT parsing error is logged at WARN level and silently swallowed —
 * the filter chain continues without authentication, and Spring Security will
 * return a 401 response via the configured {@link JwtAuthenticationEntryPoint}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX         = "Bearer ";

    private final JwtService                jwtService;
    private final CustomUserDetailsService  userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         filterChain) throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtService.extractUsername(token);

            // Only set auth if username is present and SecurityContext is empty
            if (StringUtils.hasText(username)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, username) && userDetails.isEnabled()) {
                    int tokenVer = jwtService.extractTokenVersion(token);
                    int userVer  = ((CustomUserDetails) userDetails).getTokenVersion();

                    if (tokenVer != userVer) {
                        log.warn("Token version mismatch for '{}' (token={}, current={}) — session invalidated",
                                username, tokenVer, userVer);
                    } else {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities());

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        log.debug("Authenticated user '{}' for request [{}]",
                                username, request.getRequestURI());
                    }
                }
            }
        } catch (Exception e) {
            // Do not re-throw: allow the filter chain to continue.
            // Spring Security will return 401 via JwtAuthenticationEntryPoint.
            log.warn("Could not authenticate request [{}]: {}",
                    request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the raw token string from the Authorization header.
     *
     * @return the token string, or {@code null} if the header is absent or not a Bearer token
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
