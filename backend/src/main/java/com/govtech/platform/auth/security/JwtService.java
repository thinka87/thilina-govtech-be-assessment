package com.govtech.platform.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Handles JWT token generation, parsing, and validation.
 *
 * <p>Uses the JJWT 0.12.x API with HMAC-SHA256 signing. The signing key is
 * derived from the {@code app.security.jwt.secret} property — never hardcoded.</p>
 *
 * <p><strong>Production note:</strong> The secret must be at least 32 characters
 * (256 bits) for HS256. Use a strong, randomly generated value stored in a
 * secrets manager (e.g. AWS Secrets Manager, HashiCorp Vault).</p>
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.security.jwt.secret}")
    private String secret;

    @Value("${app.security.jwt.expiration-ms}")
    private long expirationMs;

    /**
     * Generates a signed JWT for the given user.
     *
     * @param username the subject (login username)
     * @param role     the user's role (stored as a custom claim for reference)
     * @return a compact, signed JWT string
     */
    public String generateToken(String username, String role, int tokenVersion) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("ver", tokenVersion)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the token version ({@code ver} claim) from the JWT.
     * Returns {@code -1} if the claim is absent (tokens issued before versioning was added).
     */
    public int extractTokenVersion(String token) {
        Integer ver = getClaims(token).get("ver", Integer.class);
        return ver != null ? ver : -1;
    }

    /**
     * Extracts the username (subject) from a JWT.
     *
     * @param token the compact JWT string
     * @return the username stored in the token subject
     * @throws JwtException if the token is malformed or the signature is invalid
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Validates that the token belongs to the given user and has not expired.
     *
     * @param token    the compact JWT string
     * @param username the expected subject
     * @return {@code true} if the token is valid and belongs to {@code username}
     */
    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = getClaims(token);
            boolean subjectMatches = claims.getSubject().equals(username);
            boolean notExpired     = claims.getExpiration().after(new Date());
            return subjectMatches && notExpired;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Parses and verifies the token signature, returning the claims payload.
     * Throws a {@link JwtException} subclass if the token is invalid or expired.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Derives the HMAC-SHA256 signing key from the configured secret string.
     * Called on every operation — the key object is lightweight and not cached
     * to avoid holding sensitive material in memory longer than necessary.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
