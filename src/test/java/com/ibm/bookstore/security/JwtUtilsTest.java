package com.ibm.bookstore.security;

import com.ibm.bookstore.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilsTest {

    JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        // 32+ byte key required by JJWT for HS256
        props.setSecret("test-secret-key-minimum-32-bytes!!");
        props.setExpirationMs(86_400_000L);
        jwtUtils = new JwtUtils(props);
    }

    @Test
    @DisplayName("Generated token should be valid and extractable")
    void shouldGenerateValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtils.generateToken(userId, "testuser", "ROLE_CUSTOMER");

        assertThat(token).isNotBlank();
        assertThat(jwtUtils.validateToken(token)).isTrue();
        assertThat(jwtUtils.extractUsername(token)).isEqualTo("testuser");
        assertThat(jwtUtils.extractRole(token)).isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Token with wrong signature should fail validation")
    void shouldRejectTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtils.generateToken(userId, "testuser", "ROLE_CUSTOMER");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThat(jwtUtils.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("Empty or null token string should fail validation")
    void shouldRejectBlankToken() {
        assertThat(jwtUtils.validateToken("")).isFalse();
        assertThat(jwtUtils.validateToken("not.a.jwt")).isFalse();
    }

    @Test
    @DisplayName("Token signed with different secret should fail validation")
    void shouldRejectTokenFromDifferentSecret() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("completely-different-secret-key!!!");
        otherProps.setExpirationMs(86_400_000L);
        JwtUtils otherUtils = new JwtUtils(otherProps);

        String foreignToken = otherUtils.generateToken(UUID.randomUUID(), "attacker", "ROLE_CUSTOMER");

        assertThat(jwtUtils.validateToken(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void shouldRejectExpiredToken() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("test-secret-key-minimum-32-bytes!!");
        shortProps.setExpirationMs(-1L); // already expired
        JwtUtils shortLivedUtils = new JwtUtils(shortProps);

        String token = shortLivedUtils.generateToken(UUID.randomUUID(), "user", "ROLE_CUSTOMER");

        assertThat(jwtUtils.validateToken(token)).isFalse();
    }
}
