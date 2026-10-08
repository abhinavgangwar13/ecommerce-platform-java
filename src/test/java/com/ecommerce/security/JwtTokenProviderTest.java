package com.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "EcommercePlatformSecretKeyMustBeAtLeast256BitsLongForSecurity2026");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L); // 1 hour
    }

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtTokenProvider.generateToken("test@demo.com", "BUYER");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("test@demo.com", jwtTokenProvider.getEmailFromToken(token));
        assertEquals("BUYER", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.string"));
    }

    @Test
    void testTamperedToken() {
        String token = jwtTokenProvider.generateToken("test@demo.com", "BUYER");
        String tampered = token + "xyz";
        assertFalse(jwtTokenProvider.validateToken(tampered));
    }
}
