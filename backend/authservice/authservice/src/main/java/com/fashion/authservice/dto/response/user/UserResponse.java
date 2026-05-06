package com.fashion.authservice.dto.response.user;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import com.fashion.authservice.dto.response.role.RoleResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class UserResponse {

    private UUID id;

    private String email;
    private String phone;

    private Boolean active;
    private Boolean locked;

    private Boolean emailVerified;
    private Boolean phoneVerified;

    private LocalDateTime createdAt;

    private Set<RoleResponse> roles;
}