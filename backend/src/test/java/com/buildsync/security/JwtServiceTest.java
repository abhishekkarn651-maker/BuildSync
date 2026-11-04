package com.buildsync.security;

import com.buildsync.auth.domain.Role;
import com.buildsync.auth.domain.RoleName;
import com.buildsync.auth.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String TEST_SECRET = "a-very-long-secret-key-that-has-more-than-256-bits-of-entropy-for-testing!";
    private JwtService jwtService;
    private JwtConfig jwtConfig;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig(TEST_SECRET, 900);
        var secretKey = jwtConfig.jwtSecretKey();
        var encoder = jwtConfig.jwtEncoder(secretKey);
        var decoder = jwtConfig.jwtDecoder(secretKey);
        jwtService = new JwtService(encoder, decoder, jwtConfig);

        testUser = new User("Jane Developer", "janedev", "jane@example.com", "hash");
        testUser.setId(UUID.randomUUID());
        testUser.addRole(new Role(1L, RoleName.ROLE_USER.name()));
    }

    @Test
    @DisplayName("Should generate a valid HS256 JWT containing required claims")
    void testGenerateValidToken() {
        String token = jwtService.generateAccessToken(testUser);
        assertThat(token).isNotBlank();

        Jwt jwt = jwtService.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(testUser.getId().toString());
        assertThat(jwt.getClaimAsString("username")).isEqualTo("janedev");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("jane@example.com");
        assertThat(jwt.getClaimAsStringList("roles")).contains("ROLE_USER");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void testRejectInvalidSignature() {
        JwtConfig attackerConfig = new JwtConfig("another-unauthorized-secret-key-that-is-at-least-256-bits-long-32-bytes!", 900);
        var attackerKey = attackerConfig.jwtSecretKey();
        var attackerEncoder = attackerConfig.jwtEncoder(attackerKey);
        var attackerDecoder = attackerConfig.jwtDecoder(attackerKey);
        JwtService attackerJwtService = new JwtService(attackerEncoder, attackerDecoder, attackerConfig);

        String forgedToken = attackerJwtService.generateAccessToken(testUser);

        assertThatThrownBy(() -> jwtService.decode(forgedToken))
                .isInstanceOf(BadJwtException.class);
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        var secretKey = jwtConfig.jwtSecretKey();
        NimbusJwtEncoder encoder = (NimbusJwtEncoder) jwtConfig.jwtEncoder(secretKey);

        Instant issuedAt = Instant.now().minusSeconds(1200);
        Instant expiresAt = Instant.now().minusSeconds(300);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("buildsync")
                .subject(testUser.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        String expiredToken = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        assertThatThrownBy(() -> jwtService.decode(expiredToken))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should enforce at least 256 bits of key material")
    void testEnforceMinimumKeyLength() {
        assertThatThrownBy(() -> new JwtConfig("short-key", 900))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits");
    }
}
