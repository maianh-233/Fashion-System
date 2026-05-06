package com.fashion.authservice.dto.response.auth;

import com.fashion.authservice.dto.response.user.UserResponse;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    private String tokenType = "Bearer";

    private long expiresIn;

    private UserResponse user;
}
