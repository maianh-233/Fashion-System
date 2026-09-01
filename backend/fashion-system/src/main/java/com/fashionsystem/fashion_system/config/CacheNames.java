package com.fashionsystem.fashion_system.config;

/** Central registry for every Redis-backed Spring cache used by the application. */
public final class CacheNames {

    public static final String ACTIVE_MODULES = "module.active.list";
    public static final String AUTHORIZATION_MODULE_LIST = "authorization.module.list";
    public static final String AUTHORIZATION_MODULE_DETAIL = "authorization.module.detail";
    public static final String AUTHORIZATION_GROUP_LIST = "authorization.group.list";
    public static final String AUTHORIZATION_GROUP_DETAIL = "authorization.group.detail";
    public static final String AUTHORIZATION_PERMISSION_LIST = "authorization.permission.list";
    public static final String AUTHORIZATION_PERMISSION_DETAIL = "authorization.permission.detail";
    public static final String AUTHORIZATION_CATALOG = "authorization.catalog.tree";
    public static final String AUTHORIZATION_ROLE_LIST = "authorization.role.list";
    public static final String AUTHORIZATION_ROLE_DETAIL = "authorization.role.detail";
    public static final String AUTHORIZATION_EFFECTIVE_PERMISSIONS = "authorization.effective.permissions";
    public static final String AUTHORIZATION_USER_ROLE_LIST = "authorization.user-role.list";
    public static final String AUTHORIZATION_USER_PERMISSION_LIST = "authorization.user-permission.list";

    private CacheNames() {
    }
}
