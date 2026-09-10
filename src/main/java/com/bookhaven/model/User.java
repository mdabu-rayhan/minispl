package com.bookhaven.model;

import java.time.LocalDateTime;

/**
 * Base abstract domain entity representing a system user.
 */
public abstract class User {

    protected int id;
    protected String username;
    protected String passwordHash;
    protected UserRole role;
    protected LocalDateTime createdAt;

    public User() {
    }

    public User(int id, String username, String passwordHash, UserRole role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Factory method to create appropriate User subtype based on role.
     * Implements GoF Factory Method pattern by delegating to UserFactory.
     */
    public static User create(int id, String username, String passwordHash, UserRole role, LocalDateTime createdAt) {
        return UserFactory.createUser(id, username, passwordHash, role, createdAt);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public abstract boolean isAdmin();

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
