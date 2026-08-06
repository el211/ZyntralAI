package com.zyntral.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The authenticated user as seen by the rest of the app. Built statelessly from JWT
 * claims on every request — no DB lookup in the hot path. Roles are stored as
 * {@code ROLE_*} authorities so {@code @PreAuthorize("hasRole('ADMIN')")} works.
 */
public class AppPrincipal implements UserDetails {

    private final UUID userId;
    private final String email;
    private final List<GrantedAuthority> authorities;

    public AppPrincipal(UUID userId, String email, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.authorities = roles.stream()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
