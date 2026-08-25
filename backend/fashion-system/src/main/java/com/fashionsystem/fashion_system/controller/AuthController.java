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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Cung cấp các API đăng ký, đăng nhập và đăng xuất. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Validate request rồi tạo tài khoản customer và profile tương ứng.
     *
     * @param request thông tin đăng ký customer
     * @return JWT và thông tin customer với HTTP 201
     */
    @PostMapping("/register/customer")
    public ResponseEntity<CustomerAuthResponse> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCustomer(request));
    }

    /** Đăng nhập riêng cho khách hàng; không dùng tài khoản nhân viên. */
    @PostMapping("/login/customer")
    public ResponseEntity<CustomerAuthResponse> loginCustomer(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginCustomer(request));
    }

    /** Đăng ký hoặc đăng nhập khách hàng bằng Google ID token hoặc Facebook access token. */
    @PostMapping("/login/customer/social")
    public ResponseEntity<CustomerAuthResponse> loginSocialCustomer(
            @Valid @RequestBody SocialLoginRequest request) {
        return ResponseEntity.ok(authService.loginSocialCustomer(request));
    }

    /** Tạo nhân viên cùng role/phòng ban; chỉ người có quyền USER_CREATE mới được gọi. */
    @PostMapping("/register/employee")
    @PreAuthorize("hasAuthority('USER_CREATE')")
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
    @PreAuthorize("hasAuthority('USER_CREATE_ADMIN')")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterAdminRequest request) {
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
        return ResponseEntity.ok(authService.login(request));
    }

    /** Alias rõ nghĩa cho đăng nhập nhân viên; /login vẫn được giữ tương thích. */
    @PostMapping("/login/employee")
    public ResponseEntity<AuthResponse> loginEmployee(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Xác nhận logout; client phải xóa JWT vì server không duy trì session hoặc blacklist.
     *
     * @return hướng dẫn hoàn tất logout phía client
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@RequestHeader("Authorization") String authorization) {
        if (!authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Authorization Bearer token không hợp lệ"));
        }
        return ResponseEntity.ok(authService.logout(authorization.substring(7)));
    }
}
