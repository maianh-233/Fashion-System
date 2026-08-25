package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.auth.MessageResponse;
import com.fashionsystem.fashion_system.dto.auth.PasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.ResetPasswordRequest;
import com.fashionsystem.fashion_system.dto.auth.VerifyPasswordResetOtpRequest;
import com.fashionsystem.fashion_system.dto.auth.VerifyPasswordResetOtpResponse;
import com.fashionsystem.fashion_system.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API công khai cho luồng quên mật khẩu bằng OTP email. */
@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    @PostMapping("/request-otp")
    public ResponseEntity<MessageResponse> requestOtp(
            @Valid @RequestBody PasswordResetOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.requestOtp(request));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(
            @Valid @RequestBody PasswordResetOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.requestOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<VerifyPasswordResetOtpResponse> verifyOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.verifyOtp(request));
    }

    @PostMapping("/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }
}
