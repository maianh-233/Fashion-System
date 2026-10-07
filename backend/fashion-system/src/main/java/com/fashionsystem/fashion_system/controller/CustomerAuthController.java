package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.service.CustomerAuthService;
import com.fashionsystem.fashion_system.service.CustomerRefreshTokenService;
import com.fashionsystem.fashion_system.config.CustomerRefreshTokenCookieService;
import com.fashionsystem.fashion_system.exception.BusinessException;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RestController;

/** Public authentication endpoints owned exclusively by the Customer domain. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class CustomerAuthController {
    private final CustomerAuthService customerAuthService;
    private final CustomerRefreshTokenService refreshTokenService;
    private final CustomerRefreshTokenCookieService cookieService;

    @PostMapping("/register/customer")
    public ResponseEntity<CustomerAuthResponse> register(
            @Valid @RequestBody RegisterCustomerRequest request) {
        return withRefreshCookie(HttpStatus.CREATED, customerAuthService.register(request));
    }

    @PostMapping("/login/customer")
    public ResponseEntity<CustomerAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return withRefreshCookie(HttpStatus.OK, customerAuthService.login(request));
    }

    @PostMapping("/login/customer/social")
    public ResponseEntity<CustomerAuthResponse> loginSocial(
            @Valid @RequestBody SocialLoginRequest request) {
        return withRefreshCookie(HttpStatus.OK, customerAuthService.loginSocial(request));
    }

    @PostMapping("/customer/refresh")
    public ResponseEntity<CustomerAuthResponse> refresh(
            @CookieValue(name = "${auth.customer-refresh-token.cookie-name:lunaria_customer_refresh_token}", required = false)
                    String refreshToken,
            @RequestHeader(name = "X-Requested-With", required = false) String requestedWith) {
        requireAjaxHeader(requestedWith);
        try {
            CustomerRefreshTokenService.RefreshedSession refreshed = refreshTokenService.refresh(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookieService.create(refreshed.refreshToken()).toString())
                    .body(refreshed.response());
        } catch (BusinessException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                    .build();
        }
    }

    @PostMapping("/customer/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @CookieValue(name = "${auth.customer-refresh-token.cookie-name:lunaria_customer_refresh_token}", required = false)
                    String refreshToken) {
        refreshTokenService.revoke(refreshToken);
        MessageResponse response = customerAuthService.logout(
                authorization != null && authorization.startsWith("Bearer ")
                        ? authorization.substring(7) : null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .body(response);
    }

    private ResponseEntity<CustomerAuthResponse> withRefreshCookie(
            HttpStatus status, CustomerAuthResponse response) {
        String refresh = refreshTokenService.issueForCustomer(response.customer().id()).value();
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieService.create(refresh).toString())
                .body(response);
    }

    private void requireAjaxHeader(String value) {
        if (!"XMLHttpRequest".equals(value)) {
            throw BusinessException.forbidden("Refresh request không hợp lệ");
        }
    }
}
