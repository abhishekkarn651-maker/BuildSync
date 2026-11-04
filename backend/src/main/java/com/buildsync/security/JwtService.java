package com.buildsync.security;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for generating and parsing signed JWT access tokens using Spring Security's Nimbus implementation.
 */
@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtConfig jwtConfig;

    public JwtService(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder, JwtConfig jwtConfig) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.jwtConfig = jwtConfig;
    }

    /**
     * Generates a signed HS256 JWT access token for the given user with standard claims.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtConfig.getExpirationSeconds());

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("buildsync")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
    }

    /**
     * Decodes and cryptographically validates the JWT access token.
     */
    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    /**
     * Extracts the user UUID subject from a validated token.
     */
    public UUID extractUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public long getExpirationSeconds() {
        return jwtConfig.getExpirationSeconds();
    }
}

