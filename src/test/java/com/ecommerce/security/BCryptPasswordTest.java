package com.ecommerce.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

public class BCryptPasswordTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testPasswordHashingAndVerification() {
        String rawPassword = "password123";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        // Verify it starts with standard BCrypt prefix $2a$ or $2b$
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
        assertNotEquals(rawPassword, encoded);

        // Verification matches
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("wrongPassword", encoded));
    }
}
