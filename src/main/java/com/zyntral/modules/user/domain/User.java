package com.zyntral.modules.user.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Application user / identity root. Tenancy is expressed through workspace membership,
 * not on the user itself. Passwords are BCrypt hashes; {@code passwordHash} is null for
 * pure-OAuth accounts.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(nullable = false)
    private String locale = "en";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)          // maps to PG enum user_status
    @Column(nullable = false, columnDefinition = "user_status")
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {}

    public static User createLocal(String email, String passwordHash, String fullName, String locale) {
        User u = new User();
        u.email = normalizeEmail(email);
        u.passwordHash = passwordHash;
        u.fullName = fullName;
        u.locale = locale == null ? "en" : locale;
        u.status = UserStatus.PENDING;
        return u;
    }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    /** Emails are identity keys — always stored/compared lower-cased and trimmed. */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public void markEmailVerified() {
        this.emailVerified = true;
        this.status = UserStatus.ACTIVE;
    }

    public void recordLogin() { this.lastLoginAt = Instant.now(); }

    public void suspend() { this.status = UserStatus.SUSPENDED; }

    public void reactivate() { this.status = UserStatus.ACTIVE; }

    public void changePassword(String newHash) { this.passwordHash = newHash; }

    public void addRole(Role role) { this.roles.add(role); }

    // --- getters ---
    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getLocale() { return locale; }
    public UserStatus getStatus() { return status; }
    public boolean isEmailVerified() { return emailVerified; }
    public Instant getLastLoginAt() { return lastLoginAt; }
    public Set<Role> getRoles() { return roles; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setLocale(String locale) { this.locale = locale; }
}
