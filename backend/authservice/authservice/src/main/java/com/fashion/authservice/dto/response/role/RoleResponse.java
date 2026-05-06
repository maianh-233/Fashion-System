package com.fashion.authservice.dto.response.role;

import java.util.Set;
import java.util.UUID;

import com.fashion.authservice.dto.response.permission.PermissionResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class RoleResponse {

    private UUID id;

    private String code;
    private String name;

    private String description;

    private Set<PermissionResponse> permissions;
}
