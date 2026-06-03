package com.example.petlife.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginUserTest {

    private LoginUser user(long roleId) {
        return new LoginUser(1L, roleId, "テスト", "test@petlife.local", "hash", true);
    }

    // ── SUPER (roleId=2) ─────────────────────────────────────────────────────

    @Test
    void superIsAdminAndSuperButNotVetOrStaff() {
        LoginUser u = user(2L);
        assertTrue(u.isSuper());
        assertTrue(u.isAdmin());
        assertFalse(u.isVet());
        assertFalse(u.isStaff());
    }

    @Test
    void superCanManageClinicalAndHasStaffAccess() {
        LoginUser u = user(2L);
        assertTrue(u.canManageClinical());
        assertTrue(u.hasStaffAccess());
        assertTrue(u.canManageOperations());
    }

    // ── ADMIN (roleId=1) ─────────────────────────────────────────────────────

    @Test
    void adminIsAdminButNotSuperOrVetOrStaff() {
        LoginUser u = user(1L);
        assertFalse(u.isSuper());
        assertTrue(u.isAdmin());
        assertFalse(u.isVet());
        assertFalse(u.isStaff());
    }

    @Test
    void adminHasStaffAccessButNotClinical() {
        LoginUser u = user(1L);
        // ADMIN は管理系操作は可能だが診療系は担当しない
        assertFalse(u.canManageClinical());
        assertTrue(u.hasStaffAccess());
        assertTrue(u.canManageOperations());
    }

    // ── VET (roleId=4) ───────────────────────────────────────────────────────

    @Test
    void vetIsVetOnlyNotAdminOrStaff() {
        LoginUser u = user(4L);
        assertFalse(u.isAdmin());
        assertFalse(u.isSuper());
        assertTrue(u.isVet());
        assertFalse(u.isStaff());
    }

    @Test
    void vetCanManageClinicalButNotOperations() {
        LoginUser u = user(4L);
        assertTrue(u.canManageClinical());
        assertTrue(u.hasStaffAccess());
        assertFalse(u.canManageOperations());
    }

    // ── STAFF (roleId=5) ─────────────────────────────────────────────────────

    @Test
    void staffIsStaffOnlyNotAdminOrVet() {
        LoginUser u = user(5L);
        assertFalse(u.isAdmin());
        assertFalse(u.isSuper());
        assertFalse(u.isVet());
        assertTrue(u.isStaff());
    }

    @Test
    void staffCanManageClinicalAndOperations() {
        LoginUser u = user(5L);
        assertTrue(u.canManageClinical());
        assertTrue(u.hasStaffAccess());
        assertTrue(u.canManageOperations());
    }

    // ── USER (roleId=3 / その他) ─────────────────────────────────────────────

    @Test
    void userHasNoPrivilegedAccess() {
        LoginUser u = user(3L);
        assertFalse(u.isAdmin());
        assertFalse(u.isSuper());
        assertFalse(u.isVet());
        assertFalse(u.isStaff());
        assertFalse(u.canManageClinical());
        assertFalse(u.hasStaffAccess());
        assertFalse(u.canManageOperations());
    }

    // ── getAuthorities ───────────────────────────────────────────────────────

    @Test
    void getAuthoritiesReturnsCorrectRolePerRoleId() {
        assertEquals("ROLE_ADMIN",  authority(1L));
        assertEquals("ROLE_SUPER",  authority(2L));
        assertEquals("ROLE_USER",   authority(3L));
        assertEquals("ROLE_VET",    authority(4L));
        assertEquals("ROLE_STAFF",  authority(5L));
        assertEquals("ROLE_USER",   authority(99L)); // unknown → USER
    }

    private String authority(long roleId) {
        return user(roleId).getAuthorities().iterator().next().getAuthority();
    }
}
