package com.fashion.authservice.dto.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter

public class RegisterRequest {
    private String email;
    private String phone;
    private String password;
}
