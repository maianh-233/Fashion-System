package com.fashion.authservice.dto.request.permission;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class CreatePermissionRequest {
    private String code;
    private String name;
    private UUID groupId;
}
