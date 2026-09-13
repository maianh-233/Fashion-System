package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.auth.AuthResponse;
import com.fashionsystem.fashion_system.dto.auth.CustomerAuthResponse;
import com.fashionsystem.fashion_system.dto.auth.EmployeeRegistrationResponse;
import com.fashionsystem.fashion_system.dto.auth.LoginRequest;
import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.RegisterAdminRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterCustomerRequest;
import com.fashionsystem.fashion_system.dto.auth.RegisterEmployeeRequest;
import com.fashionsystem.fashion_system.dto.auth.SocialLoginRequest;
import com.fashionsystem.fashion_system.service.AuthService;
import com.fashionsystem.fashion_system.service.RefreshTokenService;
import com.fashionsystem.fashion_system.config.RefreshTokenCookieService;
import com.fashionsystem.fashion_system.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/** Cung cấp các API đăng ký, đăng nhập và đăng xuất. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;

    /**
     * Validate request rồi tạo tài khoản customer và profile tương ứng.
     *
     * @param request thông tin đăng ký customer
     * @return JWT và thông tin customer với HTTP 201
     */
    @PostMapping("/register/customer")
    public ResponseEntity<CustomerAuthResponse> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {
        CustomerAuthResponse response = authService.registerCustomer(request);
        return withRefreshCookie(HttpStatus.CREATED, response,
                refreshTokenService.issueForCustomer(response.customer().id()).value());
    }

    /** Đăng nhập riêng cho khách hàng; không dùng tài khoản nhân viên. */
    @PostMapping("/login/customer")
    public ResponseEntity<CustomerAuthResponse> loginCustomer(@Valid @RequestBody LoginRequest request) {
        CustomerAuthResponse response = authService.loginCustomer(request);
        return withRefreshCookie(HttpStatus.OK, response,
                refreshTokenService.issueForCustomer(response.customer().id()).value());
    }

    /** Đăng ký hoặc đăng nhập khách hàng bằng Google ID token. */
    @PostMapping("/login/customer/social")
    public ResponseEntity<CustomerAuthResponse> loginSocialCustomer(
            @Valid @RequestBody SocialLoginRequest request) {
        CustomerAuthResponse response = authService.loginSocialCustomer(request);
        return withRefreshCookie(HttpStatus.OK, response,
                refreshTokenService.issueForCustomer(response.customer().id()).value());
    }

    /** Endpoint tương thích cũ không có Store; chỉ ALL scope được dùng để tránh bypass Employee API. */
    @PostMapping("/register/employee")
    @PreAuthorize("hasAuthority('USER_CREATE') and @authorizationService.hasPermission(authentication, 'USER_CREATE', T(com.fashionsystem.fashion_system.entity.PermissionScope).ALL)")
    public ResponseEntity<EmployeeRegistrationResponse> registerEmployee(
            @Valid @RequestBody RegisterEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEmployee(request));
    }

    /**
     * Validate request rồi tạo tài khoản có vai trò admin.
     *
     * @param request thông tin đăng ký admin
     * @return JWT và thông tin admin với HTTP 201
     */
    @PostMapping("/register/admin")
    @PreAuthorize("hasAuthority('USER_CREATE_ADMIN') and @authorizationService.hasPermission(authentication, 'USER_CREATE_ADMIN', T(com.fashionsystem.fashion_system.entity.PermissionScope).ALL)")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterAdminRequest request) {
        // Đây là thao tác quản trị tạo tài khoản khác, không thay refresh session của admin hiện tại.
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerAdmin(request));
    }

    /**
     * Validate thông tin đăng nhập rồi xác thực và phát hành JWT.
     *
     * @param request username và password
     * @return JWT và thông tin người dùng với HTTP 200
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return withRefreshCookie(HttpStatus.OK, response,
                refreshTokenService.issueForUser(response.user().id()).value());
    }

    /** Alias rõ nghĩa cho đăng nhập nhân viên; /login vẫn được giữ tương thích. */
    @PostMapping("/login/employee")
    public ResponseEntity<AuthResponse> loginEmployee(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return withRefreshCookie(HttpStatus.OK, response,
                refreshTokenService.issueForUser(response.user().id()).value());
    }

    /** Rotate refresh token trong HttpOnly cookie và cấp access token mới. */
    @PostMapping("/refresh")
    public ResponseEntity<Object> refresh(
            @CookieValue(name = "${auth.refresh-token.cookie-name:lunaria_refresh_token}", required = false)
                    String refreshToken,
            @RequestHeader(name = "X-Requested-With", required = false) String requestedWith) {
        requireAjaxHeader(requestedWith);
        try {
            RefreshTokenService.RefreshedSession refreshed = refreshTokenService.refresh(refreshToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.create(refreshed.refreshToken()).toString())
                    .body(refreshed.response());
        } catch (BusinessException exception) {
            return ResponseEntity.status(exception.getStatusCode())
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clear().toString())
                    .body(Map.of("message", exception.getReason()));
        }
    }

    /**
     * Xác nhận logout; client phải xóa JWT vì server không duy trì session hoặc blacklist.
     *
     * @return hướng dẫn hoàn tất logout phía client
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @CookieValue(name = "${auth.refresh-token.cookie-name:lunaria_refresh_token}", required = false)
                    String refreshToken) {
        refreshTokenService.revoke(refreshToken);
        MessageResponse response = authService.logout(
                authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.clear().toString())
                .body(response);
    }

    private <T> ResponseEntity<T> withRefreshCookie(HttpStatus status, T body, String refreshToken) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieService.create(refreshToken).toString())
                .body(body);
    }

    private void requireAjaxHeader(String requestedWith) {
        if (!"XMLHttpRequest".equals(requestedWith)) {
            throw com.fashionsystem.fashion_system.exception.BusinessException.forbidden(
                    "Refresh request không hợp lệ");
        }
    }
}
