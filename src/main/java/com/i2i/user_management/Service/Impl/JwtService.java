package com.i2i.user_management.Service.Impl;

import com.i2i.user_management.Model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for JWT token generation, validation, and extraction.
 *
 * @author Sabarinathan
 */
@Service
public class    JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret.key}")
    private String SECRET;

    @Value("${jwt.expiry.in.minutes}")
    private long VALIDITY;

    /**
     * Generates a JWT token for the given user.
     *
     * @param user User entity for which the token is generated.
     * @return Signed JWT token string.
     */
    public String generateToken(User user) {
        if (user == null || user.getEmail() == null) {
            throw new IllegalArgumentException("User details cannot be null when generating token");
        }

        try {
            SecretKey key = getSigningKey();
            long validityInMillis = TimeUnit.MINUTES.toMillis(VALIDITY);
            Instant now = Instant.now();

            String token = Jwts.builder()
                    .subject(user.getEmail())
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusMillis(validityInMillis)))
                    .signWith(key)
                    .compact();

            logger.info("JWT generated successfully for user: {}", user.getEmail());
            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Error while generating JWT token", e);
        }
    }

    /**
     * Extracts username (email) from JWT token.
     *
     * @param jwt JWT token string.
     * @return Username/email encoded in the token.
     */
    public String extractUsername(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("JWT token cannot be null or blank");
        }
        try {
            return getClaims(jwt).getSubject();
        } catch (ExpiredJwtException e) {
            logger.warn("Cannot extract username — token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            logger.warn("Invalid JWT while extracting username: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Validates the JWT token by checking its expiration.
     *
     * @param jwt JWT token string.
     * @return true if valid and not expired, otherwise false.
     */
    public boolean isTokenValid(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            logger.warn("Empty JWT token received for validation");
            return false;
        }
        try {
            Claims claims = getClaims(jwt);
            boolean valid = claims.getExpiration().after(new Date());
            if (!valid) {
                logger.warn("JWT token expired for subject: {}", claims.getSubject());
            }
            return valid;
        } catch (ExpiredJwtException e) {
            logger.warn("Token expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            logger.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the token expiration date and converts it to LocalDateTime.
     *
     * @param jwt JWT token string.
     * @return Expiration time as LocalDateTime.
     */
    public LocalDateTime getTokenExpiry(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            throw new IllegalArgumentException("JWT token cannot be null or blank");
        }
        try {
            Claims claims = getClaims(jwt);
            Date expiryDate = claims.getExpiration();
            return expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (Exception e) {
            logger.warn("Error extracting expiry from JWT: {}", e.getMessage());
            throw new RuntimeException("Error extracting token expiry", e);
        }
    }

    /**
     * Parses claims from the provided JWT token.
     *
     * @param jwt JWT token string.
     * @return Parsed claims.
     */
    private Claims getClaims(String jwt) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(jwt)
                    .getPayload();
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error parsing JWT claims: {}", e.getMessage(), e);
            throw new RuntimeException("Error while parsing token claims", e);
        }
    }

    /**
     * Generates a SecretKey from the Base64 encoded secret.
     *
     * @return SecretKey for signing and verification.
     */
    private SecretKey getSigningKey() {
        try {
            byte[] decodedKey = Base64.getDecoder().decode(SECRET);
            return Keys.hmacShaKeyFor(decodedKey);
        } catch (Exception e) {
            logger.error("Invalid secret key format: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid secret key configuration", e);
        }
    }
}
