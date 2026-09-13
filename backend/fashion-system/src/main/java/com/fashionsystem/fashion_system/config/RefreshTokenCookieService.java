package com.fashionsystem.fashion_system.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenCookieService {
    private final RefreshTokenProperties properties;

    public ResponseCookie create(String token) {
        return baseCookie(token)
                .maxAge(properties.getExpiration())
                .build();
    }

    public ResponseCookie clear() {
        return baseCookie("").maxAge(0).build();
    }

    public String getCookieName() {
        return properties.getCookieName();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(properties.getCookiePath());
    }
}
