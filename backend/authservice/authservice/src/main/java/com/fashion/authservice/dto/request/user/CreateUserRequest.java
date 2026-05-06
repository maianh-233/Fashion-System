package com.fashion.authservice.dto.request.user;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class CreateUserRequest {
    private String email;
    private String phone;
    private String password;
}