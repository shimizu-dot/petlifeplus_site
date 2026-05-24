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
            case 3 -> "ROLE_VET";
            case 4 -> "ROLE_STAFF";
            default -> "ROLE_USER";
        };
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }

    public boolean isAdmin()  { return roleId == 1L; }
    public boolean isVet()   { return roleId == 3L; }
    public boolean isStaff() { return roleId == 4L; }

    /** 診療記録・診療予約・カレンダーを操作できるロール（VET + STAFF）。 */
    public boolean canManageClinical() { return isVet() || isStaff(); }

    /** ユーザ管理画面を閲覧できるロール（全スタッフ系）。 */
    public boolean hasStaffAccess() { return isAdmin() || isVet() || isStaff(); }

    /** 予約枠・お知らせを管理できるロール（ADMIN + STAFF）。 */
    public boolean canManageOperations() { return isAdmin() || isStaff(); }

    /** @deprecated canManageClinical() を使用してください。ADMIN を含む点に注意。 */
    @Deprecated
    public boolean canManagePets() { return isAdmin() || isVet() || isStaff(); }
}
