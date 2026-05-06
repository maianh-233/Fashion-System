package com.fashion.authservice.dto.request.role;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class AssignRoleRequest {
    private UUID userId;
    private UUID roleId;
}