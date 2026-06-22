package com.govtech.platform.config;

import com.govtech.platform.auth.security.CustomAccessDeniedHandler;
import com.govtech.platform.auth.security.CustomUserDetailsService;
import com.govtech.platform.auth.security.JwtAuthenticationEntryPoint;
import com.govtech.platform.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Central Spring Security configuration for the platform.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li><strong>Stateless:</strong> No HTTP session is created — every request
 *       must carry a valid JWT Bearer token.</li>
 *   <li><strong>CSRF disabled:</strong> Stateless REST APIs are not vulnerable
 *       to CSRF; disabling it removes unnecessary overhead.</li>
 *   <li><strong>{@code @EnableMethodSecurity}:</strong> Enables {@code @PreAuthorize}
 *       and {@code @PostAuthorize} annotations on service/controller methods
 *       for fine-grained role-based access control.</li>
 *   <li><strong>Custom entry points:</strong> 401 and 403 responses follow the
 *       platform's {@link com.govtech.platform.common.response.ErrorResponse} JSON format.</li>
 * </ul>
 *
 * <h2>Public endpoints</h2>
 * <ul>
 *   <li>POST /v1/auth/login — token acquisition</li>
 *   <li>/swagger-ui/**, /v3/api-docs/** — API documentation</li>
 * </ul>
 *
 * <p>All other endpoints require a valid JWT token. Role-level access is enforced
 * at the service layer via {@code @PreAuthorize}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter    jwtAuthenticationFilter;
    private final CustomUserDetailsService   userDetailsService;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler  accessDeniedHandler;

    // ── Beans ─────────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ── Security filter chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // Disable CSRF — not needed for stateless JWT APIs
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session — no HttpSession is created or used
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Authorization rules
            .authorizeHttpRequests(auth -> auth
                    // ── Public ──────────────────────────────────────────────
                    .requestMatchers(HttpMethod.POST, "/v1/auth/login").permitAll()
                    // Swagger / OpenAPI UI
                    .requestMatchers(
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/v3/api-docs",
                            "/v3/api-docs/**"
                    ).permitAll()
                    // ── Everything else requires authentication ───────────
                    .anyRequest().authenticated()
            )

            // Custom handlers for 401 and 403
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
            )

            // Register DAO auth provider
            .authenticationProvider(authenticationProvider())

            // Place JWT filter before the username/password filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
