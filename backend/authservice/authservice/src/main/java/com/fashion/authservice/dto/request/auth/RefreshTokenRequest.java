package com.fashion.authservice.dto.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class RefreshTokenRequest {
    private String refreshToken;
}
