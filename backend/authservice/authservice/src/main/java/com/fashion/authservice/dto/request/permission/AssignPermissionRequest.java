package com.fashion.authservice.dto.request.permission;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class AssignPermissionRequest {
    private UUID roleId;
    private UUID permissionId;
}
