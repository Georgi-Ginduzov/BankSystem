package com.banksystem.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Set private fields via reflection (since we don't have Spring properties in test)
        ReflectionTestUtils.setField(jwtUtil, "secret", "mySecretKeyForJWTTokenGeneration2025BankSystemSuperSecretKey1234567890");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L);
    }

    @Test
    void generateToken_returnsValidToken() {
        String token = jwtUtil.generateToken("admin@bank.com", "ADMIN");
        assertThat(token).isNotBlank();

        Claims claims = jwtUtil.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("admin@bank.com");
        assertThat(claims.get("role")).isEqualTo("ADMIN");
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("admin@bank.com", "ADMIN");
        String email = jwtUtil.extractEmail(token);
        assertThat(email).isEqualTo("admin@bank.com");
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtUtil.generateToken("admin@bank.com", "ADMIN");
        String role = jwtUtil.extractRole(token);
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("admin@bank.com", "ADMIN");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("admin@bank.com", "ADMIN");
        String tampered = token.substring(0, token.length() - 2) + "XX";
        assertThat(jwtUtil.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        // Override expiration to 1ms to force expiry
        ReflectionTestUtils.setField(jwtUtil, "expiration", 1L);
        String token = jwtUtil.generateToken("admin@bank.com", "ADMIN");
        // Wait a bit
        try {
            Thread.sleep(10);
        } catch (InterruptedException ignored) {}
        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }
}