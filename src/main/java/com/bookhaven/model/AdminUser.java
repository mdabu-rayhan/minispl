package com.bookhaven.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing an Administrator with catalog management and analytics privileges.
 */
public class AdminUser extends User {

    public AdminUser() {
        super();
        this.role = UserRole.ADMIN;
    }

    public AdminUser(int id, String username, String passwordHash, LocalDateTime createdAt) {
        super(id, username, passwordHash, UserRole.ADMIN, createdAt);
    }

    @Override
    public boolean isAdmin() {
        return true;
    }
}
