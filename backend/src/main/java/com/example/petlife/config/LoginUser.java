package com.example.petlife.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record LoginUser(
        Long id,
        Long roleId,
        String displayName,
        String email,
        String passwordHash
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = switch (roleId.intValue()) {
            case 1 -> "ROLE_ADMIN";
            case 3 -> "ROLE_VET";
            case 4 -> "ROLE_STAFF";
            default -> "ROLE_USER";
        };
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }

    public boolean isAdmin() { return roleId == 1L; }
}
