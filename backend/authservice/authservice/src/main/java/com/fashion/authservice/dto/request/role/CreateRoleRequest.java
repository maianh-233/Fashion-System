package com.fashion.authservice.dto.request.role;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class CreateRoleRequest {
    private String code;
    private String name;
    private String description;
}
