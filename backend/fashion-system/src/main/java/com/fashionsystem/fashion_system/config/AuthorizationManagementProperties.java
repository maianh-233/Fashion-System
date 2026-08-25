package com.fashionsystem.fashion_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Permission bảo vệ API quản trị, cấu hình được mà không hard-code trong controller. */
@Component("authorizationManagementProperties")
@ConfigurationProperties(prefix = "authorization.management")
public class AuthorizationManagementProperties {
    private String permissionCode = "AUTHORIZATION_MANAGE";

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}
