package com.solvix.backend.security;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private String username;

    @Column
    private String passwordHash;

    @Column(name = "auth0_subject", unique = true)
    private String auth0Subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Version
    private long version;

    protected UserEntity() {
    }

    public UserEntity(String username, String passwordHash, UserRole role) {
        this(username, passwordHash, role, null, true);
    }

    public UserEntity(String username, String passwordHash, UserRole role, String auth0Subject) {
        this(username, passwordHash, role, auth0Subject, true);
    }

    public UserEntity(String username, String passwordHash, UserRole role, String auth0Subject, boolean enabled) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.auth0Subject = auth0Subject;
        this.enabled = enabled;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAuth0Subject() {
        return auth0Subject;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
