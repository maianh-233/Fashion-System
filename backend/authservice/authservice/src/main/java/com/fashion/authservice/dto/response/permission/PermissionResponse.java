package com.fashion.authservice.dto.response.permission;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class PermissionResponse {

    private UUID id;

    private String code;
    private String name;

    private String groupName;
}
