package com.bookhaven.model;

import java.time.LocalDateTime;

/**
 * Factory for creating polymorphic User domain entities based on user role.
 * Implements the GoF Factory Method / Static Factory pattern to decouple subclass
 * instantiation (AdminUser vs ReaderUser) from persistence and business services.
 */
public class UserFactory {

    /**
     * Creates an appropriate User subclass (AdminUser or ReaderUser) based on the specified role.
     *
     * @param id           database primary key
     * @param username     account username
     * @param passwordHash bcrypt hashed password
     * @param role         UserRole enum (ADMIN or READER)
     * @param createdAt    account registration timestamp
     * @return concrete AdminUser or ReaderUser instance
     */
    public static User createUser(int id, String username, String passwordHash, UserRole role, LocalDateTime createdAt) {
        if (role == null) {
            throw new IllegalArgumentException("User role cannot be null.");
        }

        return switch (role) {
            case ADMIN -> new AdminUser(id, username, passwordHash, createdAt);
            case READER -> new ReaderUser(id, username, passwordHash, createdAt);
        };
    }

    /**
     * Creates an appropriate User subclass from a role String representation.
     */
    public static User createUser(int id, String username, String passwordHash, String roleString, LocalDateTime createdAt) {
        UserRole role = UserRole.fromString(roleString);
        return createUser(id, username, passwordHash, role, createdAt);
    }

    /**
     * Convenience factory method to instantiate a new Reader user prior to database persistence.
     */
    public static User createReader(String username, String passwordHash) {
        return new ReaderUser(0, username, passwordHash, LocalDateTime.now());
    }

    /**
     * Convenience factory method to instantiate a new Admin user prior to database persistence.
     */
    public static User createAdmin(String username, String passwordHash) {
        return new AdminUser(0, username, passwordHash, LocalDateTime.now());
    }
}
