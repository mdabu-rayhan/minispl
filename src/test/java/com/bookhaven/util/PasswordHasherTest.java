package com.bookhaven.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying BCrypt PasswordHasher functionality.
 */
public class PasswordHasherTest {

    @Test
    public void testHashAndVerifyPassword() {
        String plain = "mySecretPassword123!";
        String hash = PasswordHasher.hashPassword(plain);

        assertNotNull(hash);
        assertNotEquals(plain, hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"), "Must be a valid BCrypt hash format");
        assertTrue(PasswordHasher.checkPassword(plain, hash));
        assertFalse(PasswordHasher.checkPassword("wrongPassword", hash));
    }

    @Test
    public void testInvalidPasswordInputs() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hashPassword(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hashPassword("   "));
        assertFalse(PasswordHasher.checkPassword(null, "someHash"));
        assertFalse(PasswordHasher.checkPassword("pwd", null));
        assertFalse(PasswordHasher.checkPassword("pwd", "invalid_hash_string"));
    }
}
