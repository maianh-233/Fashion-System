package com.fashion.authservice.dto.response.auth;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
}
