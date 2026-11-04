package com.buildsync.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Spring Security JWT Configuration.
 * Configures NimbusJwtEncoder and NimbusJwtDecoder with HS256 HMAC-SHA256 signing.
 * Validates that JWT_SECRET provides at least 256 bits (32 bytes) of key material on startup.
 */
@Configuration
public class JwtConfig {

    private final String jwtSecret;
    private final long expirationSeconds;

    public JwtConfig(
            @Value("${buildsync.security.jwt.secret}") String jwtSecret,
            @Value("${buildsync.security.jwt.expiration-seconds:900}") long expirationSeconds) {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET must be configured. Key cannot be null or empty.");
        }
        byte[] secretBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must provide at least 256 bits (32 bytes) of cryptographic key material. Provided bytes: " + secretBytes.length
            );
        }
        this.jwtSecret = jwtSecret.trim();
        this.expirationSeconds = expirationSeconds;
    }

    @Bean
    public SecretKey jwtSecretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey secretKey) {
        JWKSource<SecurityContext> immutableSecret = new ImmutableSecret<>(secretKey);
        return new NimbusJwtEncoder(immutableSecret);
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}

