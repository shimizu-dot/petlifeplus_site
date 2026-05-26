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
        String passwordHash,
        boolean enabled
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = switch (roleId.intValue()) {
            case 1 -> "ROLE_ADMIN";
            case 2 -> "ROLE_SUPER";
            case 4 -> "ROLE_VET";
            case 5 -> "ROLE_STAFF";
            default -> "ROLE_USER";
        };
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }

    public boolean isSuper() { return roleId == 2L; }
    public boolean isAdmin()  { return roleId == 1L || isSuper(); }
    public boolean isVet()   { return roleId == 4L || isSuper(); }
    public boolean isStaff() { return roleId == 5L || isSuper(); }

    /** 診療記録・診療予約・カレンダーを操作できるロール（VET + STAFF）。 */
    public boolean canManageClinical() { return isVet() || isStaff(); }

    /** ユーザ管理画面を閲覧できるロール（全スタッフ系）。 */
    public boolean hasStaffAccess() { return isAdmin() || isVet() || isStaff(); }

    /** 予約枠・お知らせを管理できるロール（ADMIN + STAFF）。 */
    public boolean canManageOperations() { return isAdmin() || isStaff(); }
}
