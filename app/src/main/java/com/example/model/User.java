package com.example.model;

/**
 * Domain entity representing an authenticated user in TaskFlow.
 * Sensitive data such as plaintext passwords are never represented or held here.
 */
public class User {
    private final long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String passwordSalt;
    private final int hashIterations;
    private final long createdAt;

    public User(long id, String username, String email, String passwordHash, String passwordSalt, int hashIterations, long createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.hashIterations = hashIterations;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public int getHashIterations() {
        return hashIterations;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
