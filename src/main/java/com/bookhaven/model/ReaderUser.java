package com.bookhaven.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing a standard Reader user.
 */
public class ReaderUser extends User {

    public ReaderUser() {
        super();
        this.role = UserRole.READER;
    }

    public ReaderUser(int id, String username, String passwordHash, LocalDateTime createdAt) {
        super(id, username, passwordHash, UserRole.READER, createdAt);
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
}
