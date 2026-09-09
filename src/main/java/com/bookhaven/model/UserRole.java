package com.bookhaven.model;

/**
 * Role identifiers for Role-Based Access Control (RBAC).
 */
public enum UserRole {
    ADMIN,
    READER;

    public static UserRole fromString(String role) {
        if (role == null) return READER;
        try {
            return UserRole.valueOf(role.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return READER;
        }
    }
}
