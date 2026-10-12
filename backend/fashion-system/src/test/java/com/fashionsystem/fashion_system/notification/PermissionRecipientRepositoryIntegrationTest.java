package com.fashionsystem.fashion_system.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Executes the recipient CTE against PostgreSQL; fixtures roll back after each test. */
@SpringBootTest
@Transactional
class PermissionRecipientRepositoryIntegrationTest {

    @Autowired PermissionRecipientRepository repository;
    @Autowired JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    private final String permission = "TEST_NOTIFY_" + suffix;
    private final String inactiveModulePermission = "TEST_NOTIFY_OFF_" + suffix;
    private UUID store;
    private UUID otherStore;
    private UUID storeRole;
    private UUID allRole;

    @BeforeEach
    void seedCatalog() {
        UUID permissionId = permission(module(true), permission);
        UUID inactivePermissionId = permission(module(false), inactiveModulePermission);
        store = insertId("INSERT INTO stores (name) VALUES (?) RETURNING id", "Store " + suffix);
        otherStore = insertId("INSERT INTO stores (name) VALUES (?) RETURNING id", "Other " + suffix);
        storeRole = role("STORE_" + suffix, permissionId, "STORE");
        allRole = role("ALL_" + suffix, permissionId, "ALL");
        UUID inactiveRole = role("OFF_" + suffix, inactivePermissionId, "ALL");
        UUID inactiveModuleUser = user("off");
        assign(inactiveModuleUser, store);
        grant(inactiveModuleUser, inactiveRole);
    }

    @Test
    void resolvesEffectivePermissionWithinStoreIdentityScope() {
        UUID storeApprover = user("store");
        assign(storeApprover, store);
        grant(storeApprover, storeRole);

        UUID otherStoreApprover = user("other");
        assign(otherStoreApprover, otherStore);
        grant(otherStoreApprover, allRole);

        UUID globalApprover = user("global");
        grant(globalApprover, allRole);

        UUID globalWithStoreScope = user("global-store");
        grant(globalWithStoreScope, storeRole);

        UUID multiStore = user("multi");
        assign(multiStore, store);
        assign(multiStore, otherStore);
        grant(multiStore, allRole);

        UUID locked = user("locked");
        jdbc.update("UPDATE users SET locked = true WHERE id = ?", locked);
        assign(locked, store);
        grant(locked, allRole);

        UUID actor = user("actor");
        assign(actor, store);
        grant(actor, storeRole);

        assertThat(repository.findEligibleUserIds(permission, store, actor))
                .containsExactlyInAnyOrder(storeApprover, globalApprover);
    }

    @Test
    void directOverridesReplaceRoleGrants() {
        UUID denied = user("denied");
        assign(denied, store);
        grant(denied, allRole);
        override(denied, "DENY", "ALL");

        UUID directAllow = user("direct");
        assign(directAllow, store);
        override(directAllow, "ALLOW", "STORE");

        UUID narrowedAllow = user("narrowed");
        assign(narrowedAllow, store);
        grant(narrowedAllow, allRole);
        override(narrowedAllow, "ALLOW", "SELF");

        assertThat(repository.findEligibleUserIds(permission, store, null))
                .containsExactly(directAllow);
    }

    @Test
    void ignoresPermissionsOfInactiveModules() {
        assertThat(repository.findEligibleUserIds(inactiveModulePermission, store, null)).isEmpty();
    }

    private UUID module(boolean active) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO modules (id, code, name, active, sort_order, created_at)
                VALUES (?, ?, ?, ?, 999, now())
                """, id, "TEST_MOD_" + id.toString().substring(0, 8), "Test module", active);
        return id;
    }

    private UUID permission(UUID moduleId, String code) {
        UUID groupId = insertId(
                "INSERT INTO permission_groups (code, name, module_id) VALUES (?, ?, ?) RETURNING id",
                "GRP_" + code, "Test group", moduleId);
        return insertId("INSERT INTO permissions (code, name, group_id) VALUES (?, ?, ?) RETURNING id",
                code, "Test permission", groupId);
    }

    private UUID role(String code, UUID permissionId, String scope) {
        UUID roleId = insertId("INSERT INTO roles (code, name) VALUES (?, ?) RETURNING id",
                "TEST_ROLE_" + code, "Test role");
        jdbc.update("INSERT INTO role_permissions (role_id, permission_id, scope) VALUES (?, ?, ?)",
                roleId, permissionId, scope);
        return roleId;
    }

    private UUID user(String label) {
        return insertId("INSERT INTO users (username, active, locked) VALUES (?, true, false) RETURNING id",
                "test-" + label + "-" + suffix.toLowerCase());
    }

    private void assign(UUID userId, UUID storeId) {
        jdbc.update("INSERT INTO store_staffs (user_id, store_id, active) VALUES (?, ?, true)", userId, storeId);
    }

    private void grant(UUID userId, UUID roleId) {
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private void override(UUID userId, String effect, String scope) {
        jdbc.update("""
                INSERT INTO user_permissions (user_id, permission_id, effect, scope)
                SELECT ?, id, ?, ? FROM permissions WHERE code = ?
                """, userId, effect, scope, permission);
    }

    private UUID insertId(String sql, Object... args) {
        return jdbc.queryForObject(sql, UUID.class, args);
    }
}
