package com.bookhaven.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserFactoryTest {

    @Test
    @DisplayName("UserFactory creates AdminUser with proper role and admin flag")
    void testCreateAdmin() {
        User admin = UserFactory.createAdmin("superadmin", "hashed_pwd");
        assertNotNull(admin);
        assertEquals("superadmin", admin.getUsername());
        assertEquals("hashed_pwd", admin.getPasswordHash());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.isAdmin());
        assertInstanceOf(AdminUser.class, admin);
    }

    @Test
    @DisplayName("UserFactory creates ReaderUser with proper role and non-admin flag")
    void testCreateReader() {
        User reader = UserFactory.createReader("bookworm", "hashed_pwd");
        assertNotNull(reader);
        assertEquals("bookworm", reader.getUsername());
        assertEquals("hashed_pwd", reader.getPasswordHash());
        assertEquals(UserRole.READER, reader.getRole());
        assertFalse(reader.isAdmin());
        assertInstanceOf(ReaderUser.class, reader);
    }

    @Test
    @DisplayName("UserFactory creates correct instance from role enum or string")
    void testCreateUserWithId() {
        LocalDateTime now = LocalDateTime.now();
        User admin = UserFactory.createUser(1, "admin1", "hash", UserRole.ADMIN, now);
        assertEquals(1, admin.getId());
        assertTrue(admin.isAdmin());
        assertInstanceOf(AdminUser.class, admin);

        User reader = UserFactory.createUser(2, "reader2", "hash", "READER", now);
        assertEquals(2, reader.getId());
        assertFalse(reader.isAdmin());
        assertInstanceOf(ReaderUser.class, reader);
    }

    @Test
    @DisplayName("UserFactory defaults null or unrecognized role to READER safely")
    void testUnrecognizedRoleDefaultsToReader() {
        User fallback = UserFactory.createUser(3, "guest", "hash", (String) null, LocalDateTime.now());
        assertEquals(UserRole.READER, fallback.getRole());
        assertFalse(fallback.isAdmin());
        assertInstanceOf(ReaderUser.class, fallback);
    }
}
