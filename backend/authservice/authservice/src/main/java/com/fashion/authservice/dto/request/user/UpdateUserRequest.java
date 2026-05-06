package com.fashion.authservice.dto.request.user;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class UpdateUserRequest {
    private String email;
    private String phone;
    private Boolean active;
}