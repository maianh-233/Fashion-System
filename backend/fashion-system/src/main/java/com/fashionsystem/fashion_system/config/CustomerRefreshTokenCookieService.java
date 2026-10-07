package com.fashionsystem.fashion_system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerRefreshTokenCookieService {
    private final CustomerRefreshTokenProperties properties;

    public ResponseCookie create(String token) {
        return base(token).maxAge(properties.getExpiration()).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    public String getCookieName() { return properties.getCookieName(); }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true).secure(properties.isSecure()).sameSite(properties.getSameSite())
                .path(properties.getCookiePath());
    }
}

